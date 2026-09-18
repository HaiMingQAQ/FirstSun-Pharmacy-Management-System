package cn.iocoder.yudao.module.ai.controller.admin.pharmacy;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ai.controller.admin.pharmacy.vo.PharmacyAiChatReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.pharmacy.vo.PharmacyAiConfirmReqVO;
import cn.iocoder.yudao.module.ai.service.pharmacy.PharmacyAiCommandService;
import cn.iocoder.yudao.module.ai.service.pharmacy.PharmacyAiOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/pharmacy/ai")
@RequiredArgsConstructor
public class PharmacyAiController {
    private final PharmacyAiOrchestrator orchestrator;
    private final PharmacyAiCommandService commandService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/chat/message/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermission('pharmacy:ai:chat')")
    public Flux<ServerSentEvent<String>> send(@Valid @RequestBody PharmacyAiChatReqVO request) {
        return orchestrator.stream(request).map(event -> ServerSentEvent.<String>builder()
                .event(event.getType()).data(json(event.getData())).build());
    }

    @PostMapping("/command/{id}/confirm")
    @PreAuthorize("@ss.hasPermission('pharmacy:ai:chat')")
    public CommonResult<Map<String, Object>> confirm(@PathVariable("id") Long id,
                                                     @Valid @RequestBody PharmacyAiConfirmReqVO request) {
        return success(commandService.confirm(id, request.getConfirmationToken()));
    }

    @PostMapping("/command/{id}/cancel")
    @PreAuthorize("@ss.hasPermission('pharmacy:ai:chat')")
    public CommonResult<Boolean> cancel(@PathVariable("id") Long id) {
        commandService.cancel(id);
        return success(true);
    }

    @GetMapping("/command/recent")
    @PreAuthorize("@ss.hasPermission('pharmacy:ai:chat')")
    public CommonResult<List<PharmacyAiCommandService.Preview>> recent() {
        return success(commandService.recent());
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { return "{\"message\":\"事件序列化失败\"}"; }
    }
}
