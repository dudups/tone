package com.ezone.ezproject.modules.project.data

import com.ezone.ezproject.dal.entity.Plan
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter

class PlanChartDataParser {
    Map parse(Aggregations aggs, List<Plan> plans, List<String> enableCardTypeKeys) {
        Map result = initMap(plans, enableCardTypeKeys)
        def filters = aggs.sort { it.name }
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def name = dateFilter.name.split("_")
            def planId = name[0]
            Map<String, Object> countChange = (Map) result.get("countChange")
            Map<String, Object> planCountChange = countChange.get(planId)
            if ("end" == name[1]) {
                Map<String, Object> endRateMap = (Map) result.get("endRate")
                Map<String, Object> planEndRate = endRateMap.get(planId);
                planEndRate.put("total", dateFilter.docCount)
                planCountChange.put("end", dateFilter.docCount)
                for (int j = 0; j < dateFilter.aggregations.size(); j++) {
                    def subFilter = dateFilter.aggregations[j] as ParsedFilter
                    def subAggName = subFilter.name
                    if ("end" == subAggName) {//总的完成率
                        planEndRate.put("end", subFilter.docCount)
                    } else {
                        Map<String, Object> typeEndRate = result.get(subAggName + "EndRate")
                        Map<String, Object> planTYpeEndRate = typeEndRate.get(planId)
                        planTYpeEndRate.put("total", subFilter.docCount)
                        def subSubFilter = subFilter.getAggregations().get(subAggName + "_end") as ParsedFilter
                        planTYpeEndRate.put("end", subSubFilter.docCount)
                    }
                }
            } else if ("start" == name[1]) {
                planCountChange.put("start", dateFilter.docCount)
            }
        }
        return result
    }

    Map<String, Object> initMap(List<Plan> plans, List<String> enableCardTypeKeys) {
        def result = new HashMap();
        HashMap<String, Object> countChange = new HashMap<>();
        HashMap<String, Object> endRate = new HashMap<>();
        for (String typeKey : enableCardTypeKeys) {
            HashMap<String, Object> typeHashMap = new HashMap<>();
            for (Plan plan : plans) {
                HashMap<String, Object> planEndRate = new HashMap<>();
                planEndRate.put("total", 0);
                planEndRate.put("end", 0);
                typeHashMap.put(plan.getId().toString(), planEndRate);
            }
            result.put(typeKey + "EndRate", typeHashMap);
        }
        for (Plan plan : plans) {
            Map<String, Object> planCountChange = new HashMap<>();
            planCountChange.put("start", 0L);
            planCountChange.put("end", 0L);
            countChange.put(plan.getId().toString(), planCountChange);

            Map<String, Object> planEndChange = new HashMap<>();
            planEndChange.put("total", 0L);
            planEndChange.put("end", 0L);
            endRate.put(plan.getId().toString(), planEndChange);
        }
        result.put("endRate", endRate);
        result.put("countChange", countChange);
        return result
    }
}