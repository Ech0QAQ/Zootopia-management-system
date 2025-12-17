package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.StarCandidate;
import com.zootopia.entity.StarVote;
import com.zootopia.mapper.StarCandidateMapper;
import com.zootopia.mapper.StarVoteMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/star")
public class StarController {
    @Autowired
    private StarCandidateMapper starCandidateMapper;
    @Autowired
    private StarVoteMapper starVoteMapper;

    private static final String[] DEFAULT_CANDIDATES = {
        "朱迪", "尼克", "夏奇羊", "盖瑞", "宝伯特", "闪电", "豹警官", "牛局长"
    };
    private static final String[] DEFAULT_PHOTOS = {
        "Judy&Nick.jpg", "Judy&Nick.jpg", "Gazelle.jpg", "Gary.jpg",
        "Pawbert.jpg", "Flash.jpg", "Benjamin.jpg", "Chief.jpg"
    };

    @PostConstruct
    public void initCandidates() {
        long count = starCandidateMapper.selectCount(null);
        if (count == 0) {
            for (int i = 0; i < DEFAULT_CANDIDATES.length; i++) {
                StarCandidate candidate = new StarCandidate();
                candidate.setName(DEFAULT_CANDIDATES[i]);
                candidate.setPhoto(DEFAULT_PHOTOS[i]);
                starCandidateMapper.insert(candidate);
            }
        }
    }

    @GetMapping("/candidates")
    public Result<List<StarCandidate>> getCandidates() {
        List<StarCandidate> candidates = starCandidateMapper.selectList(null);
        return Result.success(candidates);
    }

    @GetMapping("/my-vote")
    public Result<StarCandidate> getMyVote(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        StarVote vote = starVoteMapper.selectOne(
                new LambdaQueryWrapper<StarVote>()
                        .eq(StarVote::getUserId, userId)
        );
        if (vote == null) {
            return Result.success(null);
        }
        StarCandidate candidate = starCandidateMapper.selectById(vote.getCandidateId());
        return Result.success(candidate);
    }

    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> getStats() {
        // 这里需要自定义SQL查询统计票数，简化处理
        List<StarCandidate> candidates = starCandidateMapper.selectList(null);
        List<Map<String, Object>> stats = new java.util.ArrayList<>();
        
        for (StarCandidate candidate : candidates) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("id", candidate.getId());
            stat.put("name", candidate.getName());
            stat.put("photo", candidate.getPhoto());
            
            Long votes = starVoteMapper.selectCount(
                    new LambdaQueryWrapper<StarVote>()
                            .eq(StarVote::getCandidateId, candidate.getId())
            );
            stat.put("votes", votes);
            stats.add(stat);
        }
        
        stats.sort((a, b) -> Long.compare((Long) b.get("votes"), (Long) a.get("votes")));
        return Result.success(stats);
    }

    @PostMapping("/vote")
    @Transactional
    public Result<String> vote(@RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer candidateId = request.get("candidate_id");
        if (candidateId == null) {
            return Result.error("candidate_id 必填");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        
        // 删除旧投票
        starVoteMapper.delete(
                new LambdaQueryWrapper<StarVote>()
                        .eq(StarVote::getUserId, userId)
        );

        // 插入新投票
        StarVote vote = new StarVote();
        vote.setUserId(userId);
        vote.setCandidateId(candidateId);
        starVoteMapper.insert(vote);

        return Result.success("投票成功");
    }
}

