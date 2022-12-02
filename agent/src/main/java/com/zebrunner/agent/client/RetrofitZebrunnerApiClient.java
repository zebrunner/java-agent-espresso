package com.zebrunner.agent.client;

import com.zebrunner.agent.client.request.JsonPatchRequestItem;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RetrofitZebrunnerApiClient {

    @POST("/api/iam/v1/auth/refresh")
    Call<AutenticationData> refreshToken(@Body Map<String, String> refreshToken);

    @POST("/api/reporting/v1/test-runs")
    Call<TestRunDTO> startTestRun(@Query("projectKey") String projectKey,
                                  @Header("Authorization") String token,
                                  @Body TestRunDTO testRunDTO);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests")
    Call<TestDTO> startTest(@Path("testRunId") String testRunId,
                            @Query("headless") boolean headless,
                            @Header("Authorization") String token,
                            @Body TestDTO test);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<TestDTO> updateTest(@Path("testRunId") String testRunId,
                             @Path("testId") String testId,
                             @Query("headless") boolean headless,
                             @Header("Authorization") String token,
                             @Body TestDTO testDTO);

    @PUT("/api/reporting/v1/test-runs/{testRunId}")
    Call<String> updateTestRun(@Path("testRunId") String testRunId,
                               @Header("Authorization") String token,
                               @Body TestRunDTO testRun);

    @POST("/api/reporting/v1/test-runs/{testRunId}/logs")
    Call<String> postLogs(@Path("testRunId") String testRunId,
                          @Header("Authorization") String token,
                          @Body Collection<Log> logs);

    @DELETE("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<String> deleteTest(@Path("testRunId") String testRunId,
                            @Path("testId") String testId,
                            @Header("Authorization") String token);

    @PATCH("/api/reporting/v1/test-runs/{testRunId}")
    Call<String> patchTestRun(@Path("testRunId") String testRunId,
                              @Header("Authorization") String token,
                              @Body List<JsonPatchRequestItem> patchRequest);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/platform")
    Call<String> setTestRunPlatform(@Path("testRunId") String testRunId,
                                    @Header("Authorization") String token,
                                    @Body TestRunPlatform testRunPlatform);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}")
    Call<TestDTO> rerunTest(@Path("testRunId") String testRunId,
                            @Path("testId") String testId,
                            @Query("headless") boolean headless,
                            @Header("Authorization") String token,
                            @Body TestDTO test);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/labels")
    Call<String> attachLabelsToTest(@Path("testRunId") String testRunId,
                                    @Path("testId") String testId,
                                    @Header("Authorization") String token,
                                    @Body Map<String, Collection<LabelDTO>> labelMap);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/labels")
    Call<String> attachLabelsToTestRun(@Path("testRunId") String testRunId,
                                       @Header("Authorization") String token,
                                       @Body Map<String, Collection<LabelDTO>> labelMap);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/test-cases:upsert")
    Call<String> upsertTestCaseResults(@Path("testRunId") String testRunId,
                                       @Path("testId") String testId,
                                       @Header("Authorization") String token,
                                       @Body Map<String, Collection<TestCaseResult>> testCaseMap);

    @Headers({"Content-Type: image/png"})
    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/screenshots")
    Call<String> uploadScreenshot(@Path("testRunId") String testRunId,
                                  @Path("testId") String testId,
                                  @Header("Authorization") String token,
                                  @Header("x-zbr-screenshot-captured-at") String capturedAt,
                                  @Body RequestBody screenshot);

    @Multipart
    @POST("/api/reporting/v1/test-runs/{testRunId}/artifacts")
    Call<String> uploadTestRunArtifact(@Path("testRunId") String testRunId,
                                       @Header("Authorization") String token,
                                       @Part MultipartBody.Part filePart);

    @Multipart
    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/artifacts")
    Call<String> uploadTestArtifact(@Path("testRunId") String testRunId,
                                    @Path("testId") String testId,
                                    @Header("Authorization") String token,
                                    @Part MultipartBody.Part filePart);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/artifact-references")
    Call<String> attachArtifactReferenceToTestRun(@Path("testRunId") String testRunId,
                                                  @Header("Authorization") String token,
                                                  @Body Map<String, List<ArtifactReferenceDTO>> requestBody);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/artifact-references")
    Call<String> attachArtifactReferenceToTest(@Path("testRunId") String testRunId,
                                               @Path("testId") String testId,
                                               @Header("Authorization") String token,
                                               @Body Map<String, List<ArtifactReferenceDTO>> requestBody);

    @POST("/api/reporting/v1/run-context-exchanges")
    Call<ExchangeRunContextResponse> exchangeRerunCondition(@Header("Authorization") String token,
                                                            @Body String rerunCondition);

    @POST("/api/reporting/v1/test-runs/{testRunId}/test-sessions")
    Call<TestSessionDTO> startSession(@Path("testRunId") String testRunId,
                                      @Header("Authorization") String token,
                                      @Body TestSessionDTO testSession);

    @PUT("/api/reporting/v1/test-runs/{testRunId}/test-sessions/{testSessionId}")
    Call<String> updateSession(@Path("testRunId") String testRunId,
                               @Path("testSessionId") String testSessionId,
                               @Header("Authorization") String token,
                               @Body TestSessionDTO testSession);

    @POST("/api/reporting/v1/test-runs/{testRunId}/tests/{testId}/known-issue-confirmations")
    Call<KnownIssueConfirmation> confirmIssue(@Path("testRunId") String testRunId,
                                              @Path("testId") String testId,
                                              @Header("Authorization") String token,
                                              @Body Map<String, String> failureRequest);

}
