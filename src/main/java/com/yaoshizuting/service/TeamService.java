package com.yaoshizuting.service;

import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.entity.User;
import java.util.List;

public interface TeamService {
    List<TeamNodeDTO> getTeamTree(Long userId);

    void evictTeamTreeCaches(User user);
}
