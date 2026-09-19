package cn.iocoder.yudao.module.ai.service.pharmacy;

import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PharmacyAiOrchestrator} 决策解析与有限重试测试。
 * 覆盖：标准 JSON、Markdown ```json 代码块、前后解释文字、缺 action、垃圾输入、有限重试。
 */
class PharmacyAiOrchestratorTest {

    private final AiModelService modelService = mock(AiModelService.class);
    private final PharmacyAiToolService toolService = mock(PharmacyAiToolService.class);
    private final PharmacyAiCommandService commandService = mock(PharmacyAiCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private PharmacyAiOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PharmacyAiOrchestrator(modelService, toolService, commandService, objectMapper);
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Test
    void parseStandardJsonDecision() {
        JsonNode decision = orchestrator.parseDecision(
                "{\"action\":\"prepareUpdateDrug\",\"arguments\":{\"id\":163101,\"changes\":{\"retailPrice\":25}}}");
        assertEquals("prepareUpdateDrug", decision.path("action").asText());
        assertEquals(163101, decision.path("arguments").path("id").asLong());
        assertEquals(25, decision.path("arguments").path("changes").path("retailPrice").asInt());
    }

    @Test
    void parseMarkdownJsonFence() {
        JsonNode decision = orchestrator.parseDecision("""
                ```json
                {"action":"prepareUpdateDrug","arguments":{"id":163101,"changes":{"retailPrice":25}}}
                ```
                """);
        assertEquals("prepareUpdateDrug", decision.path("action").asText());
    }

    @Test
    void parseJsonWithLeadingText() {
        JsonNode decision = orchestrator.parseDecision("好的，我将为您修改药品价格："
                + "{\"action\":\"prepareUpdateDrug\",\"arguments\":{\"id\":163101,\"changes\":{\"retailPrice\":25}}}");
        assertEquals("prepareUpdateDrug", decision.path("action").asText());
    }

    @Test
    void parseJsonWithLeadingAndTrailingText() {
        JsonNode decision = orchestrator.parseDecision("已为您生成预览。"
                + "{\"action\":\"prepareUpdateDrug\",\"arguments\":{\"id\":163101,\"changes\":{\"retailPrice\":25}}}"
                + "，请点击确认按钮完成修改。");
        assertEquals("prepareUpdateDrug", decision.path("action").asText());
        assertEquals(163101, decision.path("arguments").path("id").asLong());
    }

    @Test
    void parseJsonWithoutActionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.parseDecision("{\"arguments\":{\"id\":1}}"));
    }

    @Test
    void parseGarbageInputFailsWithActionableMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orchestrator.parseDecision("抱歉，我不太理解你的意思。"));
        assertTrue(ex.getMessage().contains("重新描述"), ex.getMessage());
    }

    @Test
    void routeDecisionRetriesOnceWhenFirstOutputIsUnparseable() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(response("让我先查一下药品信息……"))
                .thenReturn(response("{\"action\":\"prepareUpdateDrug\",\"arguments\":{\"id\":1,\"changes\":{\"retailPrice\":25}}}"));

        JsonNode decision = orchestrator.routeDecision(chatModel, mock(ChatOptions.class), "把药品 1 的零售价改为 25 元");

        assertEquals("prepareUpdateDrug", decision.path("action").asText());
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void routeDecisionStopsAfterLimitedRetries() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(response("第一次不可解析"))
                .thenReturn(response("第二次仍不可解析"));

        assertThrows(IllegalStateException.class,
                () -> orchestrator.routeDecision(chatModel, mock(ChatOptions.class), "把药品 1 的零售价改为 25 元"));
        verify(chatModel, times(2)).call(any(Prompt.class)); // 初始 1 次 + 有限重试 1 次，禁止无限重试
    }
}
