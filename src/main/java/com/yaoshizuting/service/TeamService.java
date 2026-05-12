package com.yaoshizuting.service;

import com.yaoshizuting.dto.TeamNodeDTO;
import java.util.List;

public interface TeamService {
    List<TeamNodeDTO> getTeamTree(Long userId);
}
