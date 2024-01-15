package ee.tenman.elektrihind.apollo;

import lombok.Getter;

@Getter
public enum Cinema {

    ULEMISTE("Ülemiste", "https://www.apollokino.ee/schedule?theatreAreaID=1017"),
    MUSTAMAE("Mustamäe", "https://www.apollokino.ee/schedule?theatreAreaID=1007");

    private final String name;
    private final String url;

    Cinema(String name, String url) {
        this.name = name;
        this.url = url;
    }

}
