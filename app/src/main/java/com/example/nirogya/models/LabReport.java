package com.example.nirogya.models;

public class LabReport {

    private String id;

    private String patientId;
    private String patientName;
    private String reportTitle;
    private String reportType;
    private String technicianName;
    private String technicianId;
    private Long timestamp;

    // CBC
    private String hemoglobin;
    private String wbc;
    private String platelets;

    // Lipid
    private String hdl;
    private String ldl;
    private String triglycerides;

    // Blood Sugar
    private String fastingSugar;
    private String postSugar;
    private String hba1c;

    private String remarks; // ✅ New field for remarks

    public LabReport() {
        // Required for Firestore
    }

    // ✅ Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getReportTitle() { return reportTitle; }
    public String getReportType() { return reportType; }
    public String getTechnicianName() { return technicianName; }
    public String getTechnicianId() { return technicianId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getHemoglobin() { return hemoglobin; }
    public String getWbc() { return wbc; }
    public String getPlatelets() { return platelets; }

    public String getHdl() { return hdl; }
    public String getLdl() { return ldl; }
    public String getTriglycerides() { return triglycerides; }

    public String getFastingSugar() { return fastingSugar; }
    public String getPostSugar() { return postSugar; }
    public String getHba1c() { return hba1c; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    // ✅ Auto-generate remark
    public String generateRemark() {
        StringBuilder remark = new StringBuilder();

        try {
            if ("CBC".equals(reportType)) {
                double hb = Double.parseDouble(hemoglobin);
                double wbcCount = Double.parseDouble(wbc);
                double plt = Double.parseDouble(platelets);

                if (hb < 13 || hb > 17) remark.append("Hemoglobin out of range. ");
                if (wbcCount < 4 || wbcCount > 11) remark.append("WBC abnormal. ");
                if (plt < 150 || plt > 400) remark.append("Platelets abnormal. ");
            }

            else if ("Lipid Profile".equals(reportType)) {
                double hdlVal = Double.parseDouble(hdl);
                double ldlVal = Double.parseDouble(ldl);
                double tgVal = Double.parseDouble(triglycerides);

                if (hdlVal < 40) remark.append("Low HDL. ");
                if (ldlVal > 100) remark.append("High LDL. ");
                if (tgVal > 150) remark.append("High Triglycerides. ");
            }

            else if ("Blood Sugar".equals(reportType)) {
                double fasting = Double.parseDouble(fastingSugar);
                double post = Double.parseDouble(postSugar);
                double a1c = Double.parseDouble(hba1c);

                if (fasting < 70 || fasting > 100) remark.append("Fasting sugar abnormal. ");
                if (post > 140) remark.append("Postprandial sugar high. ");
                if (a1c > 5.7) remark.append("HbA1c elevated. ");
            }

        } catch (NumberFormatException e) {
            remark.append("Invalid/missing values.");
        }

        return remark.length() > 0 ? remark.toString() : "All parameters normal.";
    }
}
