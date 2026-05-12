package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.GytUserHierarchy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GytUserHierarchyMapper extends BaseMapper<GytUserHierarchy> {
    // 获取指定用户的树状路径
    String selectPathByUserId(@Param("userId") Long userId);

    // 查询路径前缀下的全部节点（包含自身）
    List<GytUserHierarchy> selectByPathPrefix(@Param("path") String path);
}
