package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.service.TeamService;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamControllerTest {

    @Test
    void getTeamTreeExtractsBearerTokenAndReturnsServiceTree() {
        TeamService teamService = mock(TeamService.class);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        TeamController controller = new TeamController(teamService, jwtUtils);
        TeamNodeDTO node = new TeamNodeDTO();
        node.setUserId(20L);
        node.setParentId(10L);
        node.setRole(1);

        when(jwtUtils.getUserIdFromToken("token-value")).thenReturn(10L);
        when(teamService.getTeamTree(10L)).thenReturn(List.of(node));

        ApiResponse<List<TeamNodeDTO>> response = controller.getTeamTree("Bearer token-value");

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals(20L, response.getData().get(0).getUserId());
        verify(jwtUtils).getUserIdFromToken("token-value");
        verify(teamService).getTeamTree(10L);
    }
}
