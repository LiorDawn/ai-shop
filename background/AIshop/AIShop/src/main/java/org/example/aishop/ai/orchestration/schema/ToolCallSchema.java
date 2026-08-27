package org.example.aishop.ai.orchestration.schema;

import java.util.List;
import java.util.Map;

/**
 * JSON Schema 定义 — 约束大模型工具调用输出格式
 *
 * 大模型只能按此结构输出 JSON，不能自由发挥。
 * 包含完整工具列表及每个工具的参数约束。
 */
public class ToolCallSchema {

    /** 工具名称 */
    public static final String FIELD_TOOL_NAME = "toolName";
    /** 工具参数 */
    public static final String FIELD_PARAMETERS = "parameters";
    /** 是否需要继续执行下一个工具 */
    public static final String FIELD_NEED_CONTINUE = "needContinueTool";

    // ==================== 购物车工具 ====================
    public static final String TOOL_CART_QUERY = "cart_query";
    public static final String TOOL_CART_ADD = "cart_add";
    public static final String TOOL_CART_DELETE = "cart_delete";
    public static final String TOOL_CART_UPDATE_NUM = "cart_update_num";
    public static final String TOOL_CART_CHECK_ALL = "cart_check_all";

    // ==================== 商品工具 ====================
    public static final String TOOL_PRODUCT_QUERY = "product_query";
    public static final String TOOL_PRODUCT_RECOMMEND = "product_recommend";
    public static final String TOOL_PRODUCT_DETAIL = "product_detail";

    // ==================== 订单工具 ====================
    public static final String TOOL_ORDER_QUERY = "order_query";
    public static final String TOOL_ORDER_DETAIL = "order_detail";

    // ==================== 售后工具 ====================
    public static final String TOOL_AFTERSALE_QUERY = "aftersale_query";
    public static final String TOOL_AFTERSALE_DETAIL = "aftersale_detail";

    /** ★ 停止工具调用，大模型调用此工具即终止工具循环 */
    public static final String TOOL_STOP = "stop";

    /**
     * 构建 System Prompt 中注入的工具列表 JSON Schema
     *
     * 大模型看到此 Schema 后，只能输出符合格式的 JSON。
     */
    public static String buildToolSchemaPrompt() {
        return """
                【可用工具列表】
                你只能使用以下工具，toolName 必须从列表中选取。

                === 购物车操作 ===
                1. cart_query — 查询用户购物车全部商品
                   参数：无
                   示例：{"toolName":"cart_query","parameters":{},"needContinueTool":false}

                2. cart_add — 添加商品到购物车
                   参数：productId (必填, 商品ID), skuId (可选, SKU ID), num (可选, 数量, 默认1)
                   示例：{"toolName":"cart_add","parameters":{"productId":123,"num":1},"needContinueTool":false}

                3. cart_delete — 删除购物车中指定商品
                   参数：productId (必填, 商品ID)
                   示例：{"toolName":"cart_delete","parameters":{"productId":123},"needContinueTool":false}

                4. cart_update_num — 修改购物车商品数量
                   参数：productId (必填, 商品ID), num (必填, 新数量)
                   示例：{"toolName":"cart_update_num","parameters":{"productId":123,"num":3},"needContinueTool":false}

                5. cart_check_all — 全选/全不选购物车
                   参数：checked (必填, 1=全选, 0=全不选)
                   示例：{"toolName":"cart_check_all","parameters":{"checked":1},"needContinueTool":false}

                === 商品查询 ===
                6. product_query — 搜索商品（按名称/分类）
                   参数：keyword (可选, 搜索关键词), categoryId (可选, 分类ID), sort (可选, 0=默认 1=销量 2=价格 3=新品)
                   示例：{"toolName":"product_query","parameters":{"keyword":"蓝牙耳机"},"needContinueTool":false}

                7. product_recommend — 获取个性化推荐商品
                   参数：categoryId (可选, 分类ID，不传则全站推荐)
                   示例：{"toolName":"product_recommend","parameters":{},"needContinueTool":false}

                8. product_detail — 获取商品详情
                   参数：productId (必填, 商品ID，可从 product_query 或 product_recommend 结果中获取)
                   示例：{"toolName":"product_detail","parameters":{"productId":123},"needContinueTool":false}

                === 订单查询 ===
                9. order_query — 查询我的订单列表
                   参数：orderStatus (可选, 0=待付款 1=待发货 2=待收货 3=已完成)
                   示例：{"toolName":"order_query","parameters":{"orderStatus":1},"needContinueTool":false}

                10. order_detail — 获取订单详情
                   参数：orderId (必填, 订单ID，可从 order_query 结果中获取)
                   示例：{"toolName":"order_detail","parameters":{"orderId":456},"needContinueTool":false}

                === 售后查询 ===
                11. aftersale_query — 查询我的售后列表
                   参数：auditStatus (可选, 0=待审核 1=已通过 2=已驳回)
                   示例：{"toolName":"aftersale_query","parameters":{},"needContinueTool":false}

                12. aftersale_detail — 获取售后详情
                   参数：afterSaleId (必填, 售后ID，可从 aftersale_query 结果中获取)
                   示例：{"toolName":"aftersale_detail","parameters":{"afterSaleId":789},"needContinueTool":false}

                === 停止工具 ===
                13. stop — 停止工具调用，直接生成最终回复
                   参数：无
                   示例：{"toolName":"stop","parameters":{},"needContinueTool":false}

                【重要规则】
                - 如果用户需要执行操作（加购/删除/改数量/全选），toolName 必须填对应的购物车工具名
                - 如果用户询问商品/推荐/找东西，toolName 填 product_query 或 product_recommend
                - 如果用户询问订单/物流/到哪了，toolName 填 order_query 或 order_detail
                - 如果用户询问售后/退款/退货，toolName 填 aftersale_query 或 aftersale_detail
                - 如果用户说的是闲聊/打招呼，不需要操作任何工具，toolName 填 "stop"，needContinueTool 填 false
                - 如果所有工具已执行完毕，不需要再调用任何工具时，toolName 填 "stop"
                - 如果用户一次说了多个操作，先执行第一个工具，needContinueTool 填 true
                - 回复格式必须是纯 JSON，不要包含任何其他文字，不要包含 markdown 代码块标记
                - 回复格式：{"toolName":"工具名","parameters":{...},"needContinueTool":true/false}
                """;
    }

    /**
     * 提取工具列表供参数填充器使用
     * 每个工具定义其参数名、是否必填、默认值
     */
    public static Map<String, List<ParamDef>> getToolParamDefs() {
        return Map.ofEntries(
                // 购物车
                Map.entry(TOOL_CART_ADD, List.of(
                        new ParamDef("productId", true, null),
                        new ParamDef("skuId", false, null),
                        new ParamDef("num", false, 1)
                )),
                Map.entry(TOOL_CART_DELETE, List.of(
                        new ParamDef("productId", true, null)
                )),
                Map.entry(TOOL_CART_UPDATE_NUM, List.of(
                        new ParamDef("productId", true, null),
                        new ParamDef("num", true, null)
                )),
                Map.entry(TOOL_CART_CHECK_ALL, List.of(
                        new ParamDef("checked", true, null)
                )),
                Map.entry(TOOL_CART_QUERY, List.of()),
                // 商品
                Map.entry(TOOL_PRODUCT_QUERY, List.of(
                        new ParamDef("keyword", false, ""),
                        new ParamDef("categoryId", false, null),
                        new ParamDef("sort", false, 0)
                )),
                Map.entry(TOOL_PRODUCT_RECOMMEND, List.of(
                        new ParamDef("categoryId", false, null)
                )),
                Map.entry(TOOL_PRODUCT_DETAIL, List.of(
                        new ParamDef("productId", true, null)
                )),
                // 订单
                Map.entry(TOOL_ORDER_QUERY, List.of(
                        new ParamDef("orderStatus", false, null)
                )),
                Map.entry(TOOL_ORDER_DETAIL, List.of(
                        new ParamDef("orderId", true, null)
                )),
                // 售后
                Map.entry(TOOL_AFTERSALE_QUERY, List.of(
                        new ParamDef("auditStatus", false, null)
                )),
                Map.entry(TOOL_AFTERSALE_DETAIL, List.of(
                        new ParamDef("afterSaleId", true, null)
                ))
        );
    }

    /**
     * 参数定义
     */
    public record ParamDef(String name, boolean required, Object defaultValue) {}
}