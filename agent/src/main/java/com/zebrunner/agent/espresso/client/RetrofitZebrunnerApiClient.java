package com.zebrunner.agent.espresso.client;

import com.zebrunner.agent.core.logging.Log;
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
import com.zebrunner.agent.espresso.client.request.JsonPatchRequestItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitZebrunnerApiClient {

    String REFRESH_TOKEN_PATH = "/api/iam/v1/auth/refresh";

    @POST(REFRESH_TOKEN_PATH)
    Call<AutenticationData> refreshToken(@Body Map<String, String> refreshToken);

    @POST("/api/reporting/v1/test-runs")
    Call<TestRunDTO> startTestRun(@Query("projectKey") String projectKey,
                                  @Body TestRunDTO testRunDTO);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests")
    Call<TestDTO> startTest(@Path("testRunId") Long testRunId,
                            @Query("headless") boolean headless,
                            @Body TestDTO test);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<TestDTO> updateTest(@Path("testRunId") Long testRunId,
                             @Path("testId") Long testId,
                             @Query("headless") boolean headless,
                             @Body TestDTO testDTO);

    @PUT("/api/reporting/v1/test-runs/{testRunId}")
    Call<String> updateTestRun(@Path("testRunId") Long testRunId,
                               @Body TestRunDTO testRun);

    @POST("/api/reporting/v1/test-runs/{testRunId}/logs")
    Call<String> postLogs(@Path("testRunId") Long testRunId,
                          @Body Collection<Log> logs);

    @DELETE("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<String> deleteTest(@Path("testRunId") Long testRunId,
                            @Path("testId") Long testId);

    @PATCH("/api/reporting/v1/test-runs/{testRunId}")
    Call<String> patchTestRun(@Path("testRunId") Long testRunId,
                              @Body List<JsonPatchRequestItem> patchRequest);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/platform")
    Call<String> setTestRunPlatform(@Path("testRunId") Long testRunId,
                                    @Body TestRunPlatform testRunPlatform);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<TestDTO> rerunTest(@Path("testRunId") Long testRunId,
                            @Path("testId") Long testId,
                            @Query("headless") boolean headless,
                            @Body TestDTO test);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/labels")
    Call<String> attachLabelsToTest(@Path("testRunId") Long testRunId,
                                    @Path("testId") Long testId,
                                    @Body Map<String, Collection<LabelDTO>> labelMap);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/labels")
    Call<String> attachLabelsToTestRun(@Path("testRunId") Long testRunId,
                                       @Body Map<String, Collection<LabelDTO>> labelMap);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/test-cases:upsert")
    Call<String> upsertTestCaseResults(@Path("testRunId") Long testRunId,
                                       @Path("testId") Long testId,
                                       @Body Map<String, Collection<TestCaseResult>> testCaseMap);

    @Headers({"Content-Type: image/png"})
    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/screenshots")
    Call<String> uploadScreenshot(@Path("testRunId") Long testRunId,
                                  @Path("testId") Long testId,
                                  @Header("x-zbr-screenshot-captured-at") String capturedAt,
                                  @Body RequestBody screenshot);

    @Multipart
    @POST("/api/reporting/v1/test-runs/{testRunId}/artifacts")
    Call<String> uploadTestRunArtifact(@Path("testRunId") Long testRunId,
                                       @Part MultipartBody.Part filePart);

    @Multipart
    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/artifacts")
    Call<String> uploadTestArtifact(@Path("testRunId") Long testRunId,
                                    @Path("testId") Long testId,
                                    @Part MultipartBody.Part filePart);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/artifact-references")
    Call<String> attachArtifactReferenceToTestRun(@Path("testRunId") Long testRunId,
                                                  @Body Map<String, List<ArtifactReferenceDTO>> requestBody);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/artifact-references")
    Call<String> attachArtifactReferenceToTest(@Path("testRunId") Long testRunId,
                                               @Path("testId") Long testId,
                                               @Body Map<String, List<ArtifactReferenceDTO>> requestBody);

    @POST("/api/reporting/v1/run-context-exchanges")
    Call<ExchangeRunContextResponse> exchangeRerunCondition(@Body String rerunCondition);

    @POST("/api/reporting/v1/test-runs/{testRunId}/test-sessions")
    Call<TestSessionDTO> startSession(@Path("testRunId") Long testRunId,
                                      @Body TestSessionDTO testSession);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/test-sessions/{testSessionId}")
    Call<String> updateSession(@Path("testRunId") Long testRunId,
                               @Path("testSessionId") Long testSessionId,
                               @Body TestSessionDTO testSession);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/known-issue-confirmations")
    Call<KnownIssueConfirmation> confirmIssue(@Path("testRunId") Long testRunId,
                                              @Path("testId") Long testId,
                                              @Body Map<String, String> failureRequest);

}
