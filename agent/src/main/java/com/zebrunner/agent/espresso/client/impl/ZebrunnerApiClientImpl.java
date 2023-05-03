package com.zebrunner.agent.espresso.client.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zebrunner.agent.core.config.ConfigurationHolder;
import com.zebrunner.agent.core.exception.ServerException;
import com.zebrunner.agent.core.logging.Log;
import com.zebrunner.agent.core.registrar.ZebrunnerApiClient;
import com.zebrunner.agent.core.registrar.domain.ArtifactReferenceDTO;
import com.zebrunner.agent.core.registrar.domain.AutenticationData;
import com.zebrunner.agent.core.registrar.domain.ExchangeRunContextResponse;
import com.zebrunner.agent.core.registrar.domain.KnownIssueConfirmation;
import com.zebrunner.agent.core.registrar.domain.LabelDTO;
import com.zebrunner.agent.core.registrar.domain.TestCaseResult;
import com.zebrunner.agent.core.registrar.domain.TestDTO;
import com.zebrunner.agent.core.registrar.domain.TestRunDTO;
import com.zebrunner.agent.core.registrar.domain.TestRunPlatform;
import com.zebrunner.agent.core.registrar.domain.TestSessionDTO;
import com.zebrunner.agent.espresso.client.AuthorizationHeaderInterceptor;
import com.zebrunner.agent.espresso.client.RetrofitZebrunnerApiClient;
import com.zebrunner.agent.espresso.client.request.JsonPatchRequestItem;
import com.zebrunner.agent.espresso.core.converter.OffsetDateTimeConverter;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@Slf4j
public class ZebrunnerApiClientImpl implements ZebrunnerApiClient {

    private static final Set<String> AUTH_HEADER_INTERCEPTOR_EXCLUSIONS = Set.of(
            RetrofitZebrunnerApiClient.REFRESH_TOKEN_PATH
    );

    private static ZebrunnerApiClientImpl INSTANCE;

    private volatile String authToken;
    private volatile RetrofitZebrunnerApiClient client;

    public static synchronized ZebrunnerApiClientImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZebrunnerApiClientImpl();
        }
        return INSTANCE;
    }

    private ZebrunnerApiClientImpl() {
        if (ConfigurationHolder.isReportingEnabled()) {
            this.client = this.initClient();
        }
    }

    private RetrofitZebrunnerApiClient initClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthorizationHeaderInterceptor(AUTH_HEADER_INTERCEPTOR_EXCLUSIONS, this::obtainAuthToken))
                .build();

        Gson gson = new GsonBuilder().registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeConverter())
                                     .create();

        return new Retrofit.Builder().baseUrl(ConfigurationHolder.getHost())
                                     .client(okHttpClient)
                                     .addConverterFactory(ScalarsConverterFactory.create())
                                     .addConverterFactory(GsonConverterFactory.create(gson))
                                     .build()
                                     .create(RetrofitZebrunnerApiClient.class);
    }

    private synchronized String obtainAuthToken() {
        if (authToken == null) {
            authToken = this.authenticateClient();
        }

        return authToken;
    }

    @SneakyThrows
    private String authenticateClient() {
        String refreshToken = ConfigurationHolder.getToken();

        Response<AutenticationData> response = client.refreshToken(Collections.singletonMap("refreshToken", refreshToken))
                                                     .execute();
        if (!response.isSuccessful()) {
            // null out the api client since we cannot use it anymore
            client = null;
            this.throwServerException("Not able to obtain api token", response);
        }

        return response.body().getAuthTokenType() + " " + response.body().getAuthToken();
    }

    private String formatError(String message, Response<?> response) {
        return String.format(
                "%s\nResponse status code: %s.\nRaw response body: \n%s",
                message, response.code(), response.body()
        );
    }

    private void throwServerException(String message, Response<?> response) {
        throw new ServerException(this.formatError(message, response));
    }

    @Override
    @SneakyThrows
    public TestRunDTO registerTestRunStart(TestRunDTO testRun) {
        Response<TestRunDTO> response = client.startTestRun(ConfigurationHolder.getProjectKey(), testRun)
                                              .execute();
        if (!response.isSuccessful()) {
            // null out the api client since we cannot use it anymore
            client = null;
            this.throwServerException("Could not register start of the test run.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void patchTestRunBuild(Long testRunId, String build) {
        JsonPatchRequestItem item = new JsonPatchRequestItem("replace", "/config/build", build);

        Response<String> response = client.patchTestRun(testRunId, List.of(item))
                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not patch build of the test run.", response);
        }
    }

    @Override
    @SneakyThrows
    public void setTestRunPlatform(Long testRunId, String platformName, String platformVersion) {
        Response<String> response = client.setTestRunPlatform(
                                                  testRunId,
                                                  new TestRunPlatform(platformName, platformVersion)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not set platform of the test run.", response);
        }
    }

    @Override
    @SneakyThrows
    public void registerTestRunFinish(TestRunDTO testRun) {
        Response<String> response = client.updateTestRun(testRun.getId(), testRun)
                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register finish of the test run.", response);
        }
    }

    @Override
    @SneakyThrows
    public TestDTO registerTestStart(Long testRunId, TestDTO test, boolean headless) {
        Response<TestDTO> response = client.startTest(testRunId, headless, test)
                                           .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register start of the test.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestDTO registerTestRerunStart(Long testRunId, Long testId, TestDTO test, boolean headless) {
        Response<TestDTO> response = client.rerunTest(testRunId, testId, headless, test)
                                           .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register start of rerun of the test.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestDTO registerHeadlessTestUpdate(Long testRunId, TestDTO test) {
        Response<TestDTO> response = client.updateTest(testRunId, test.getId(), true, test)
                                           .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register start of the test.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void revertTestRegistration(Long testRunId, Long testId) {
        Response<String> response = client.deleteTest(testRunId, testId)
                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not revert test registration.", response);
        }
    }

    @Override
    @SneakyThrows
    public void registerTestFinish(Long testRunId, TestDTO test) {
        Response<TestDTO> response = client.updateTest(testRunId, test.getId(), false, test)
                                           .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register finish of the test.", response);
        }
    }

    @Override
    @SneakyThrows
    public void sendLogs(Collection<Log> logs, Long testRunId) {
        Response<String> response = client.postLogs(testRunId, logs)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not send a batch of test logs.", response));
        }
    }

    @Override
    @SneakyThrows
    public void upsertTestCaseResults(Long testRunId, Long testId, Collection<TestCaseResult> testCaseResults) {
        Response<String> response = client.upsertTestCaseResults(
                                                  testRunId,
                                                  testId,
                                                  Collections.singletonMap("testCases", testCaseResults)
                                          )
                                          .execute();
        if (response.code() == 404) {
            log.warn("This functionality is not available for your Zebrunner distribution");
        } else {
            log.error(this.formatError("Could not send test case results.", response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadScreenshot(byte[] screenshot, Long testRunId, Long testId, Long capturedAt) {
        Response<String> response = client.uploadScreenshot(
                                                  testRunId,
                                                  testId,
                                                  capturedAt.toString(),
                                                  RequestBody.create(MediaType.parse("image/png"), screenshot)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not upload a screenshot.", response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadTestRunArtifact(InputStream artifact, String name, Long testRunId) {
        Response<String> response = client.uploadTestRunArtifact(
                                                  testRunId,
                                                  this.toRequestPart("file", name, artifact)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach test run artifact with name " + name, response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadTestArtifact(InputStream artifact, String name, Long testRunId, Long testId) {
        Response<String> response = client.uploadTestArtifact(
                                                  testRunId,
                                                  testId,
                                                  this.toRequestPart("file", name, artifact)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach test artifact with name " + name, response));
        }
    }

    @SneakyThrows
    private MultipartBody.Part toRequestPart(String name, String fileName, InputStream inputStream) {
        byte[] body = new byte[inputStream.available()];
        inputStream.read(body);

        return MultipartBody.Part.createFormData(name, fileName, RequestBody.create(null, body));
    }

    @Override
    @SneakyThrows
    public void attachArtifactReferenceToTestRun(Long testRunId, ArtifactReferenceDTO artifactReference) {
        Response<String> response = client.attachArtifactReferenceToTestRun(
                                                  testRunId,
                                                  Collections.singletonMap("items", Collections.singletonList(artifactReference))
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following test run artifact reference: " + artifactReference, response));
        }
    }

    @Override
    @SneakyThrows
    public void attachArtifactReferenceToTest(Long testRunId, Long testId, ArtifactReferenceDTO artifactReference) {
        Response<String> response = client.attachArtifactReferenceToTest(
                                                  testRunId,
                                                  testId,
                                                  Collections.singletonMap("items", Collections.singletonList(artifactReference))
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following test artifact reference: " + artifactReference, response));
        }
    }

    @Override
    @SneakyThrows
    public void attachLabelsToTestRun(Long testRunId, Collection<LabelDTO> labels) {
        Response<String> response = client.attachLabelsToTestRun(
                                                  testRunId,
                                                  Collections.singletonMap("items", labels)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following labels to test run: " + labels, response));
        }
    }

    @Override
    @SneakyThrows
    public void attachLabelsToTest(Long testRunId, Long testId, Collection<LabelDTO> labels) {
        Response<String> response = client.attachLabelsToTest(
                                                  testRunId,
                                                  testId,
                                                  Collections.singletonMap("items", labels)
                                          )
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following labels to test: " + labels, response));
        }
    }

    @Override
    @SneakyThrows
    public ExchangeRunContextResponse exchangeRerunCondition(String rerunCondition) {
        Response<ExchangeRunContextResponse> response = client.exchangeRerunCondition(rerunCondition)
                                                              .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not get tests by ci run id.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestSessionDTO startSession(Long testRunId, TestSessionDTO testSession) {
        Response<TestSessionDTO> response = client.startSession(testRunId, testSession)
                                                  .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not register start of the test session.", response);
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void updateSession(Long testRunId, TestSessionDTO testSession) {
        Response<String> response = client.updateSession(testRunId, testSession.getId(), testSession)
                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not update test session.", response);
        }
    }

    @Override
    @SneakyThrows
    public boolean isKnownIssueAttachedToTest(Long testRunId, Long testId, String failureStacktrace) {
        Response<KnownIssueConfirmation> response = client.confirmIssue(
                                                                  testRunId,
                                                                  testId,
                                                                  Collections.singletonMap("failureReason", failureStacktrace)
                                                          )
                                                          .execute();
        if (!response.isSuccessful()) {
            this.throwServerException("Could not retrieve status of attached known issues.", response);
        }

        return response.body().isKnownIssue();
    }

}
