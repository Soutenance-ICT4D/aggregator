package com.sharepay.aggregator.modules.account.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartSeriesItem {

    /** Nom du groupe : statut, opérateur ou application */
    private String key;

    /** Nombre de transactions par slot temporel (aligné sur `labels`) */
    private List<Long> counts;

    /** Volume (netAmount) par slot temporel (aligné sur `labels`) */
    private List<Long> volumes;
}
