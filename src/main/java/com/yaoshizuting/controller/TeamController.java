package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.service.TeamService;
import com.yaoshizuting.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
    private final JwtUtils jwtUtils;

    @GetMapping("/tree")
    public ApiResponse<List<TeamNodeDTO>> getTeamTree(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtils.getUserIdFromToken(authHeader.substring(7));
        List<TeamNodeDTO> tree = teamService.getTeamTree(userId);
        return ApiResponse.success(tree);
    }
}
