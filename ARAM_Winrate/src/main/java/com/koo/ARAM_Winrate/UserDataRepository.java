package com.koo.ARAM_Winrate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, UserDataId> {
    Optional<UserData> findByGameNameAndTagLine(String gameName, String tagLine);
}