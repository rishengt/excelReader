package com.example.caseprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO for API request to consumer app
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseRequest {
    private CaseObject caseObj;

    @Data
    public static class CaseObject {
        private String title;
        private String type;
        private String status;
        private String teamCode;
        private String team;
        private String priority;
        private PrimaryParty primaryParty;
    }
    @Data
    public static class PrimaryParty {
        private String eciId;
        private String branchCode = "USA";
    }

}



