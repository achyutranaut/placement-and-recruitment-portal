package com.placement.portal.neo4j.dto;

import java.util.Map;

public class GraphMutationDto {

    public static class CreateEntityRequestDto {
        private String label;
        private Map<String, Object> properties;

        public CreateEntityRequestDto() {}
        public CreateEntityRequestDto(String label, Map<String, Object> properties) {
            this.label = label;
            this.properties = properties;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }

    public static class UpdateEntityRequestDto {
        private Map<String, Object> properties;

        public UpdateEntityRequestDto() {}
        public UpdateEntityRequestDto(Map<String, Object> properties) {
            this.properties = properties;
        }

        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }

    public static class CreateRelationshipRequestDto {
        private String fromLabel;
        private String fromId;
        private String toLabel;
        private String toId;
        private String type;

        public CreateRelationshipRequestDto() {}
        public CreateRelationshipRequestDto(String fromLabel, String fromId, String toLabel, String toId, String type) {
            this.fromLabel = fromLabel;
            this.fromId = fromId;
            this.toLabel = toLabel;
            this.toId = toId;
            this.type = type;
        }

        public String getFromLabel() { return fromLabel; }
        public void setFromLabel(String fromLabel) { this.fromLabel = fromLabel; }
        public String getFromId() { return fromId; }
        public void setFromId(String fromId) { this.fromId = fromId; }
        public String getToLabel() { return toLabel; }
        public void setToLabel(String toLabel) { this.toLabel = toLabel; }
        public String getToId() { return toId; }
        public void setToId(String toId) { this.toId = toId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class DeleteRelationshipRequestDto {
        private String fromLabel;
        private String fromId;
        private String toLabel;
        private String toId;
        private String type;

        public DeleteRelationshipRequestDto() {}
        public DeleteRelationshipRequestDto(String fromLabel, String fromId, String toLabel, String toId, String type) {
            this.fromLabel = fromLabel;
            this.fromId = fromId;
            this.toLabel = toLabel;
            this.toId = toId;
            this.type = type;
        }

        public String getFromLabel() { return fromLabel; }
        public void setFromLabel(String fromLabel) { this.fromLabel = fromLabel; }
        public String getFromId() { return fromId; }
        public void setFromId(String fromId) { this.fromId = fromId; }
        public String getToLabel() { return toLabel; }
        public void setToLabel(String toLabel) { this.toLabel = toLabel; }
        public String getToId() { return toId; }
        public void setToId(String toId) { this.toId = toId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class GraphMutationResponseDto {
        private boolean success;
        private String message;
        private Object data;

        public GraphMutationResponseDto() {}
        public GraphMutationResponseDto(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
