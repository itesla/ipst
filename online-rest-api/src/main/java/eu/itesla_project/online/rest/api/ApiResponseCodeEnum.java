package eu.itesla_project.online.rest.api;

public enum ApiResponseCodeEnum {
    ERROR("error"), WARNING("warning"), INFO("info"), OK("ok"), TOO_BUSY("too busy");

    private final String type;

    private ApiResponseCodeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
