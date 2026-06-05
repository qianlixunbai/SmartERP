package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.entity.ApprovalNode;
import com.smartoa.entity.ApprovalTemplate;
import com.smartoa.entity.TemplateField;
import com.smartoa.entity.User;
import com.smartoa.service.TemplateService;
import com.smartoa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserService userService;

    @GetMapping("/api/templates")
    public Result<List<ApprovalTemplate>> list() {
        return Result.success(templateService.listAll());
    }

    @GetMapping("/api/templates/{id}")
    public Result<ApprovalTemplate> getById(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @PostMapping("/api/templates")
    public Result<Void> create(@RequestBody ApprovalTemplate template) {
        User user = userService.getLoginUser();
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        templateService.create(template);
        return Result.success(null, "创建成功");
    }

    @PutMapping("/api/templates/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ApprovalTemplate data) {
        User user = userService.getLoginUser();
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        templateService.update(id, data);
        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/api/templates/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        templateService.delete(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/api/templates/{id}/nodes")
    public Result<List<ApprovalNode>> listNodes(@PathVariable Long id) {
        return Result.success(templateService.listNodes(id));
    }

    @PostMapping("/api/templates/{id}/nodes")
    public Result<Void> saveNodes(@PathVariable Long id, @RequestBody List<ApprovalNode> nodes) {
        User user = userService.getLoginUser();
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        templateService.saveNodes(id, nodes);
        return Result.success(null, "保存成功");
    }

    @DeleteMapping("/api/templates/{id}/nodes/{nodeId}")
    public Result<Void> deleteNode(@PathVariable Long id, @PathVariable Long nodeId) {
        User user = userService.getLoginUser();
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        templateService.deleteNode(nodeId);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/api/templates/{id}/fields")
    public Result<List<TemplateField>> listFields(@PathVariable Long id) {
        return Result.success(templateService.listFields(id));
    }
}
