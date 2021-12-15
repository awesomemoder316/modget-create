package io.github.awesomemoder316.modgetcreate.github;

import com.diogonunes.jcolor.Attribute;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.github.awesomemoder316.modgetcreate.github.json.OAuthAccessTokenJson;
import io.github.awesomemoder316.modgetcreate.github.json.OAuthVerifyCodeJson;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.diogonunes.jcolor.Ansi.colorize;

@Singleton
public class GithubTokenManager implements TokenManager {
    private final Moshi moshi;
    @SuppressWarnings("FieldCanBeLocal") private String oAuthDeviceId;
    private final OkHttpClient okHttpClient;
    private String githubToken;

    @Inject
    public GithubTokenManager(Moshi moshi, OkHttpClient okHttpClient) {
        this.moshi = moshi;
        this.okHttpClient = okHttpClient;
    }

    @Override
    public Optional<String> getToken() {
        return githubToken == null ?
                Optional.empty() : Optional.of(githubToken);
    }

    @Override
    public boolean setToken(String token) {
        if (validateToken(token)) {
            this.githubToken = token;
            return true;
        }
        return false;
    }

    @Override
    public boolean oAuth() {
        String clientId = "3cd493499ff6e8c086c9";
        try {

            RequestBody verifyCodeRequestBody = new FormBody.Builder()
                    .add("client_id", clientId)
                    .add("scope", "public_repo")
                    .build();

            Request verifyCodeRequest = new Request.Builder()
                    .addHeader("Accept", "application/json")
                    .post(verifyCodeRequestBody)
                    .url("https://github.com/login/device/code")
                    .build();

            Response verifyCodeResponse = okHttpClient.newCall(verifyCodeRequest).execute();

            JsonAdapter<OAuthVerifyCodeJson> verifyCodeAdapter = moshi.adapter(OAuthVerifyCodeJson.class);

            OAuthVerifyCodeJson verifyCodeJson = verifyCodeAdapter.fromJson(
                    Objects.requireNonNull(verifyCodeResponse.body()).string()
            );

            //Checking if it is null, so stop giving me NPE warnings.
            //noinspection ConstantConditions
            if (verifyCodeJson.getVerificationUri() == null) {
                //If this is null, resp failed. Else, all success.
                System.out.println(
                        colorize(
                                "An unexpected error occurred when trying to verify your Github account!",
                                Attribute.RED_TEXT()
                        )
                );
                return false;
            }

            System.out.println(
                    colorize(
                            "Please go to " + verifyCodeJson.getVerificationUri() +
                                    ", and enter the code ",
                            Attribute.CYAN_TEXT()
                    ) +
                            colorize(
                                    verifyCodeJson.getUserCode(),
                                    Attribute.BOLD()
                            ) +
                            colorize(
                                    ".",
                                    Attribute.CYAN_TEXT()
                            )
            );

            oAuthDeviceId = verifyCodeJson.getDeviceCode();

            RequestBody accessTokenRequestBody = new FormBody.Builder()
                    .add("client_id", clientId)
                    .add("device_code", verifyCodeJson.getDeviceCode())
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .build();

            Request accessTokenRequest = new Request.Builder()
                    .addHeader("Accept", "application/json")
                    .post(accessTokenRequestBody)
                    .url("https://github.com/login/oauth/access_token")
                    .build();

            JsonAdapter<OAuthAccessTokenJson> accessTokenAdapter = moshi.adapter(OAuthAccessTokenJson.class);

            //If another request is sent, and code changes, stop POSTing this instance.
            while (oAuthDeviceId.equals(verifyCodeJson.getDeviceCode())) {

                //noinspection BusyWait, expected behaviour
                Thread.sleep(verifyCodeJson.getInterval() * 1000L + 1000L);

                Response accessTokenResponse = okHttpClient.newCall(accessTokenRequest).execute();

                OAuthAccessTokenJson accessTokenJson = accessTokenAdapter.fromJson(
                        Objects.requireNonNull(accessTokenResponse.body()).string());

                //Checking if it is null, so stop giving me NPE warnings.
                //noinspection ConstantConditions
                if (accessTokenJson.getError() != null) {

                    switch (accessTokenJson.getError()) {
                        case "slow_down":
                            //noinspection BusyWait, expected behaviour.
                            Thread.sleep(5500L);
                        case "expired_token":
                            System.out.println(
                                    colorize(
                                            "Your token expired. Please re-run the command to get a new token.",
                                            Attribute.RED_TEXT()
                                    )
                            );
                            return false;
                        //Not checking for unsupported_grant_type.
                        case "access_denied":
                            System.out.println(
                                    colorize(
                                            "You cancelled the authentication!" +
                                                    " Please re-run the command to get a new token.",
                                            Attribute.RED_TEXT()
                                    )
                            );
                            return false;

                        default: continue;
                    }
                }

                githubToken = accessTokenJson.getAccessToken();
                System.out.println(
                        colorize(
                                "GitHub successfully authenticated!",
                                Attribute.GREEN_TEXT()
                        )
                );
                return true;
            }


        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private boolean validateToken(String token) {
        //Start by checking the scope.
        RequestBody requestBody = RequestBody.create(
                "query {" +
                        "viewer {" +
                        "   login" +
                        "}" +
                        "}",
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .url("https://api.github.com/graphql")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();

            String scopes = response.headers().get("X-OAuth-Scopes");
            if (scopes == null) return false;

            for (String scope: scopes.split(", ")){
                if (scope.equals("public_repo")) return true;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        return false;
    }
}