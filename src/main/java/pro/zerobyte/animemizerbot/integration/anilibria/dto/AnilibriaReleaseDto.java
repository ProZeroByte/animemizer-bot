package pro.zerobyte.animemizerbot.integration.anilibria.dto;

public record AnilibriaReleaseDto(Long id, AnilibriaNameDto name) {
    public record AnilibriaNameDto(String main, String english) {}
}
