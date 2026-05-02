package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionChartResponse {

    private ChartInterval interval;
    private ChartGroupBy groupBy;
    private String currency;

    /** Labels des slots temporels (heures ou jours selon l'intervalle) */
    private List<String> labels;

    /** Une série par valeur du critère de groupement */
    private List<ChartSeriesItem> series;
}
