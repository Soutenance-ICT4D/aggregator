package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.TxChartCustomType;
import com.sharepay.aggregator.shared.constant.TxChartInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxChartResponse {

    private TxChartInterval interval;
    private TxChartCustomType customType;
    private ChartGroupBy groupBy;
    private String currency;
    private List<String> labels;
    private List<ChartSeriesItem> series;
}
