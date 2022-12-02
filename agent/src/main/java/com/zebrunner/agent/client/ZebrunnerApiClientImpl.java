package com.zebrunner.agent.client;

import com.zebrunner.agent.client.request.JsonPatchRequestItem;
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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class ZebrunnerApiClientImpl implements ZebrunnerApiClient {

    private static ZebrunnerApiClientImpl INSTANCE;

    private volatile RetrofitZebrunnerApiClient client;
    private volatile String token;

    private ZebrunnerApiClientImpl() {
        if (ConfigurationHolder.isReportingEnabled()) {
            client = initClient();
        }
    }

    public static synchronized ZebrunnerApiClientImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZebrunnerApiClientImpl();
        }
        return INSTANCE;
    }

    private RetrofitZebrunnerApiClient initClient() {
        return RetrofitClientFactory.createClient(RetrofitZebrunnerApiClient.class);
    }

    private String formatError(String message, Response<?> response) {
        return String.format(
                "%s\nResponse status code: %s.\nRaw response body: \n%s",
                message, response.code(), response.body()
        );
    }

    @Override
    @SneakyThrows
    public TestRunDTO registerTestRunStart(TestRunDTO testRun) {
        String token = this.obtainToken();

        Response<TestRunDTO> response = client.startTestRun(ConfigurationHolder.getProjectKey(), token, testRun)
                                              .execute();
        if (!response.isSuccessful()) {
            // null out the api client since we cannot use it anymore
            client = null;
            throw new ServerException(this.formatError("Could not register start of the test run.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void patchTestRunBuild(Long testRunId, String build) {
        String token = this.obtainToken();
        JsonPatchRequestItem item = new JsonPatchRequestItem("replace", "/config/build", build);

        Response<String> response = client.patchTestRun(testRunId.toString(), token, List.of(item))
                                          .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not patch build of the test run.", response));
        }
    }

    @Override
    @SneakyThrows
    public void setTestRunPlatform(Long testRunId, String platformName, String platformVersion) {
        String token = this.obtainToken();

        Response<String> response = client.setTestRunPlatform(testRunId.toString(), token, new TestRunPlatform(platformName, platformVersion))
                                          .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not set platform of the test run.", response));
        }
    }

    @Override
    @SneakyThrows
    public void registerTestRunFinish(TestRunDTO testRun) {
        String token = this.obtainToken();

        Response<String> response = client.updateTestRun(testRun.getId().toString(), token, testRun)
                                          .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register finish of the test run.", response));
        }
    }

    @Override
    @SneakyThrows
    public TestDTO registerTestStart(Long testRunId, TestDTO test, boolean headless) {
        String token = this.obtainToken();

        Response<TestDTO> response = client.startTest(testRunId.toString(), headless, token, test)
                                           .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register start of the test.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestDTO registerTestRerunStart(Long testRunId, Long testId, TestDTO test, boolean headless) {
        String token = this.obtainToken();

        Response<TestDTO> response = client.rerunTest(testRunId.toString(), testId.toString(), headless, token, test)
                                           .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register start of rerun of the test.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestDTO registerHeadlessTestUpdate(Long testRunId, TestDTO test) {
        String token = this.obtainToken();

        Response<TestDTO> response = client.updateTest(testRunId.toString(), test.getId().toString(), true, token, test)
                                           .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register start of the test.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void revertTestRegistration(Long testRunId, Long testId) {
        String token = this.obtainToken();

        Response<String> response = client.deleteTest(testRunId.toString(), testId.toString(), token)
                                          .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not revert test registration.", response));
        }
    }

    @Override
    @SneakyThrows
    public void registerTestFinish(Long testRunId, TestDTO test) {
        String token = this.obtainToken();

        Response<TestDTO> response = client.updateTest(testRunId.toString(), test.getId().toString(), false, token, test)
                                           .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register finish of the test.", response));
        }
    }

    @Override
    @SneakyThrows
    public void sendLogs(Collection<Log> logs, Long testRunId) {
        String token = this.obtainToken();

        Response<String> response = client.postLogs(testRunId.toString(), token, logs)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not send a batch of test logs.", response));
        }
    }

    @Override
    @SneakyThrows
    public void upsertTestCaseResults(Long testRunId, Long testId, Collection<TestCaseResult> testCaseResults) {
        String token = this.obtainToken();

        Call<String> call = client.upsertTestCaseResults(
                testRunId.toString(), testId.toString(), token,
                Collections.singletonMap("testCases", testCaseResults)
        );
        Response<String> response = call.execute();
        if (response.code() == 404) {
            log.warn("This functionality is not available for your Zebrunner distribution");
        } else {
            log.error(this.formatError("Could not send test case results.", response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadScreenshot(byte[] screenshot, Long testRunId, Long testId, Long capturedAt) {
        String token = this.obtainToken();
        RequestBody body = RequestBody.create(screenshot, MediaType.parse("image/png"));

        Response<String> response = client.uploadScreenshot(testRunId.toString(), testId.toString(), token, capturedAt.toString(), body)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not upload a screenshot.", response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadTestRunArtifact(InputStream artifact, String name, Long testRunId) {
        String token = this.obtainToken();
        byte[] artifactBytes = new byte[artifact.available()];
        artifact.read(artifactBytes);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", name, RequestBody.create(artifactBytes));

        Response<String> response = client.uploadTestRunArtifact(testRunId.toString(), token, filePart)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach test run artifact with name " + name, response));
        }
    }

    @Override
    @SneakyThrows
    public void uploadTestArtifact(InputStream artifact, String name, Long testRunId, Long testId) {
        String token = this.obtainToken();
        byte[] artifactBytes = new byte[artifact.available()];
        artifact.read(artifactBytes);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", name, RequestBody.create(artifactBytes));

        Response<String> response = client.uploadTestArtifact(testRunId.toString(), testId.toString(), token, filePart)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach test artifact with name " + name, response));
        }
    }

    @Override
    @SneakyThrows
    public void attachArtifactReferenceToTestRun(Long testRunId, ArtifactReferenceDTO artifactReference) {
        String token = this.obtainToken();
        Map<String, List<ArtifactReferenceDTO>> requestBody = Collections.singletonMap(
                "items", Collections.singletonList(artifactReference)
        );

        Response<String> response = client.attachArtifactReferenceToTestRun(testRunId.toString(), token, requestBody)
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError(
                    "Could not attach the following test run artifact reference: " + artifactReference, response
            ));
        }
    }

    @Override
    @SneakyThrows
    public void attachArtifactReferenceToTest(Long testRunId, Long testId, ArtifactReferenceDTO artifactReference) {
        String token = this.obtainToken();

        Call<String> call = client.attachArtifactReferenceToTest(
                testRunId.toString(), testId.toString(), token,
                Collections.singletonMap("items", Collections.singletonList(artifactReference))
        );
        Response<String> response = call.execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError(
                    "Could not attach the following test artifact reference: " + artifactReference, response
            ));
        }
    }

    @Override
    @SneakyThrows
    public void attachLabelsToTestRun(Long testRunId, Collection<LabelDTO> labels) {
        String token = this.obtainToken();

        Response<String> response = client.attachLabelsToTestRun(testRunId.toString(), token, Collections.singletonMap("items", labels))
                                          .execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following labels to test run: " + labels, response));
        }
    }

    @Override
    @SneakyThrows
    public void attachLabelsToTest(Long testRunId, Long testId, Collection<LabelDTO> labels) {
        String token = this.obtainToken();

        Call<String> call = client.attachLabelsToTest(
                testRunId.toString(), testId.toString(), token,
                Collections.singletonMap("items", labels)
        );
        Response<String> response = call.execute();
        if (!response.isSuccessful()) {
            log.error(this.formatError("Could not attach the following labels to test: " + labels, response));
        }
    }

    @Override
    @SneakyThrows
    public ExchangeRunContextResponse exchangeRerunCondition(String rerunCondition) {
        String token = this.obtainToken();

        Response<ExchangeRunContextResponse> response = client.exchangeRerunCondition(token, rerunCondition)
                                                              .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not get tests by ci run id.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public TestSessionDTO startSession(Long testRunId, TestSessionDTO testSession) {
        String token = this.obtainToken();

        Response<TestSessionDTO> response = client.startSession(testRunId.toString(), token, testSession)
                                                  .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not register start of the test session.", response));
        }

        return response.body();
    }

    @Override
    @SneakyThrows
    public void updateSession(Long testRunId, TestSessionDTO testSession) {
        String token = this.obtainToken();

        Response<String> response = client.updateSession(testRunId.toString(), testSession.getId().toString(), token, testSession)
                                          .execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not update test session.", response));
        }
    }

    @Override
    @SneakyThrows
    public boolean isKnownIssueAttachedToTest(Long testRunId, Long testId, String failureStacktrace) {
        String token = this.obtainToken();

        Call<KnownIssueConfirmation> call = client.confirmIssue(
                testRunId.toString(), testId.toString(), token,
                Collections.singletonMap("failureReason", failureStacktrace)
        );
        Response<KnownIssueConfirmation> response = call.execute();
        if (!response.isSuccessful()) {
            throw new ServerException(this.formatError("Could not retrieve status of attached known issues.", response));
        }
        KnownIssueConfirmation confirmation = response.body();

        return confirmation != null && confirmation.isKnownIssue();
    }

    private String obtainToken() {
        if (token == null) {
            AutenticationData autenticationData = this.login();
            this.token = "Bearer " + autenticationData.getAuthToken();
        }
        return token;
    }

    private AutenticationData login() {
        return getAuthData();
    }

    @SneakyThrows
    private AutenticationData getAuthData() {
        String refreshToken = ConfigurationHolder.getToken();

        Response<AutenticationData> response = client.refreshToken(Collections.singletonMap("refreshToken", refreshToken))
                                                     .execute();
        if (!response.isSuccessful()) {
            // null out the api client since we cannot use it anymore
            client = null;
            throw new ServerException(this.formatError("Not able to obtain api token", response));
        }

        return response.body();
    }
}
