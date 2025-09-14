// UserData.java
package com.koo.ARAM_Winrate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// 복합키를 위한 클래스
class UserDataId implements Serializable {
    private String gameName;
    private String tagLine;
}

@Entity
@Getter
@Setter
@IdClass(UserDataId.class) // 복합키 클래스 지정
public class UserData {

    @Id
    private String gameName;

    @Id
    private String tagLine;

    private int initialScore;
    private int score;
    private int targetScore;
    private long lastUpdatedTimestamp;
}
