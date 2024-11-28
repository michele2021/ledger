package com.accounting.core.tasks;

import com.accounting.core.reports.DailySalesReportUtils;
import com.accounting.core.reports.DailySalesReportsProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DailySalesReportTaskTest {

    @Mock
    private DailySalesReportsProcessor dailySalesReportsProcessor;

    @InjectMocks
    private DailySalesReportTask dailySalesReportTask;


    @BeforeEach
    void reset() {
        Mockito.reset(dailySalesReportsProcessor);
    }

    @Test
    void testExecuteNoParams() {
        // given
        String defaultReportName = DailySalesReportUtils.getPartialReportName();
        Map<String, List<String>> parameters = Collections.emptyMap();

        // when
        dailySalesReportTask.execute(parameters, null);

        // then
        verify(dailySalesReportsProcessor).processReport(defaultReportName, false);
    }

    @Test
    void testExecuteValidReportNameParam() {
        // given
        String reportName = "some report name";
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(DailySalesReportTask.REPORT_NAME_PARAM, Collections.singletonList(reportName));

        // when
        dailySalesReportTask.execute(parameters, null);

        // then
        verify(dailySalesReportsProcessor).processReport(reportName, false);
    }

    @Test
    void testExecuteToManyReportNameParams() {
        // given
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(DailySalesReportTask.REPORT_NAME_PARAM, List.of("1 name", "2 name"));

        // when
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> dailySalesReportTask.execute(parameters, null));


        assertThat(exception.getMessage()).contains(
                format("Parameter '%s' can only be specified once", DailySalesReportTask.REPORT_NAME_PARAM));
        assertEquals(exception.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        // then
        verifyNoMoreInteractions(dailySalesReportsProcessor);
    }


    @ParameterizedTest
    @ValueSource(strings = {"TRUE", "FALSE", "true", "false", "False", "True", "TrUe", "FaLse"})
    void testExecuteValidReprocessParam(String reprocessParam) {
        // given
        String defaultReportName = DailySalesReportUtils.getPartialReportName();
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(DailySalesReportTask.REPROCESS_PARAM, Collections.singletonList(reprocessParam));

        // when
        dailySalesReportTask.execute(parameters, null);

        // then
        verify(dailySalesReportsProcessor).processReport(defaultReportName, Boolean.parseBoolean(reprocessParam));
    }

    @Test
    void testExecuteToManyReprocessParams() {
        // given
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(DailySalesReportTask.REPROCESS_PARAM, List.of("TRUE", "false"));

        // when
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> dailySalesReportTask.execute(parameters, null));


        assertThat(exception.getMessage()).contains(
                format("Parameter '%s' can only be specified once", DailySalesReportTask.REPROCESS_PARAM));
        assertEquals(exception.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        // then
        verifyNoMoreInteractions(dailySalesReportsProcessor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Invalid-Param", "flase", "TURE"})
    void testExecuteInvalidReprocessParams(String invalidParam) {
        // given
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put(DailySalesReportTask.REPROCESS_PARAM, Collections.singletonList(invalidParam));

        // when
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> dailySalesReportTask.execute(parameters, null));

        assertThat(exception.getMessage()).contains(
                format("Parameter '%s' can can only have value 'true' or 'false'.", DailySalesReportTask.REPROCESS_PARAM));
        assertEquals(exception.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        // then
        verifyNoMoreInteractions(dailySalesReportsProcessor);
    }
}
