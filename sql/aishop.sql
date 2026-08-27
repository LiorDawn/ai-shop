/*
 Navicat Premium Data Transfer

 Source Server         : root
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : aishop

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 25/08/2026 21:28:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for address
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `receiver` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收件人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收件电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '1默认地址',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收货地址' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of address
-- ----------------------------
INSERT INTO `address` VALUES (1, 1, '张三', '13800138001', '福建省莆田市城厢区龙桥街道188号', 1);
INSERT INTO `address` VALUES (2, 2, '李四', '13800138002', '福建省福州市鼓楼区五四路208号', 1);
INSERT INTO `address` VALUES (3, 3, '王五', '13800138003', '厦门市思明区软件园二期', 1);
INSERT INTO `address` VALUES (4, 4, '赵六', '13800138004', '泉州市丰泽区华大街道', 1);
INSERT INTO `address` VALUES (5, 5, '孙七', '13800138005', '漳州市龙文区万达商场', 1);
INSERT INTO `address` VALUES (7, 12, '1', '13800138111', '1', 1);

-- ----------------------------
-- Table structure for after_sale
-- ----------------------------
DROP TABLE IF EXISTS `after_sale`;
CREATE TABLE `after_sale`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL COMMENT '订单明细ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `shop_id` bigint NULL DEFAULT NULL COMMENT '店铺ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID',
  `type` tinyint NULL DEFAULT 0 COMMENT '0仅退款1退货退款2换货',
  `amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '申请退款金额',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '售后原因',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '问题描述',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '凭证图片(逗号分隔)',
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '0待审核1已通过2已驳回3待退货4已完成',
  `audit_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `return_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商家退货地址',
  `express_company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '快递公司',
  `express_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物流单号',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '0未删1已删',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_oid`(`order_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_shop_id`(`shop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '售后单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of after_sale
-- ----------------------------
INSERT INTO `after_sale` VALUES (1, 1, 1, 1, 1, 1, 0, 500.00, '质量问题', '屏幕有坏点', 'https://picsum.photos/200/200', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, '2026-06-14 10:00:00');
INSERT INTO `after_sale` VALUES (2, 3, 4, 3, 2, 16, 0, 79.90, '商品破损', '包装破损', NULL, 1, '商家', '2026-06-14 10:30:00', '同意退款', NULL, NULL, NULL, NULL, 0, '2026-06-14 09:00:00');
INSERT INTO `after_sale` VALUES (3, 6, 5, 3, 2, 22, 0, 50.00, '不想要了', '已拆封', NULL, 2, '商家', '2026-06-14 11:00:00', '不支持七天无理由', NULL, NULL, NULL, NULL, 0, '2026-06-14 08:00:00');
INSERT INTO `after_sale` VALUES (4, 2, 2, 2, 2, 2, 1, 269.00, '发错货', '收到灰色不是黑色', 'https://picsum.photos/200/202', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, '2026-06-14 12:00:00');
INSERT INTO `after_sale` VALUES (5, 6, 6, 3, 2, 28, 1, 69.90, '过敏', '脸部红肿', 'https://picsum.photos/200/203', 3, '商家', '2026-06-14 13:00:00', '同意退货请寄回', '福建省福州市鼓楼区五四路208号优品百货售后部', NULL, NULL, NULL, 0, '2026-06-14 11:30:00');
INSERT INTO `after_sale` VALUES (6, 3, 3, 3, 2, 5, 1, 69.90, '质量问题', '键盘不灵敏', 'https://picsum.photos/200/205', 4, '商家', '2026-06-13 14:00:00', '已退款', '福建省福州市鼓楼区五四路208号优品百货售后部', '顺丰速运', 'SF1234567890', '2026-06-14 15:00:00', 0, '2026-06-13 10:00:00');
INSERT INTO `after_sale` VALUES (7, 11, 11, 1, 4, 35, 0, 199.00, '音质问题', '右耳无声，已尝试重启配对', 'https://picsum.photos/200/210', 2, '商家', '2026-06-20 23:57:22', '不通过', NULL, NULL, NULL, NULL, 0, '2026-06-14 10:00:00');
INSERT INTO `after_sale` VALUES (8, 12, 12, 2, 4, 36, 0, 89.00, '充不进电', '充电宝无法给手机充电', NULL, 1, '王五', '2026-06-14 10:30:00', '同意退款，金额原路返回', NULL, NULL, NULL, NULL, 0, '2026-06-14 09:00:00');
INSERT INTO `after_sale` VALUES (9, 13, 14, 1, 4, 38, 1, 299.00, '键位手感不对', '有几个键按下去回弹不顺畅', 'https://picsum.photos/200/211', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, '2026-06-11 11:00:00');
INSERT INTO `after_sale` VALUES (10, 14, 15, 2, 4, 39, 1, 158.00, '鼠标滚轮异响', '使用两天后滚轮出现异响，申请退货退款', 'https://picsum.photos/200/212', 4, '王五', '2026-06-14 09:00:00', '已收到退货，确认退款', '福建省福州市鼓楼区五四路208号王五数码售后部，收件人：王五，13800138003', '圆通速递', 'YT9999999999', '2026-06-14 15:30:00', 0, '2026-06-13 14:00:00');
INSERT INTO `after_sale` VALUES (11, 15, 16, 1, 4, 40, 0, 25.00, '不想要了', '下单后觉得不需要了，还未发货', NULL, 2, '王五', '2026-06-14 16:30:00', '订单已进入拣货流程，无法取消', NULL, NULL, NULL, NULL, 0, '2026-06-14 16:10:00');

-- ----------------------------
-- Table structure for ai_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '所属会话id',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息文本',
  `img_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上传图片地址(多模态)',
  `role` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'user用户/assistantAI',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sid`(`session_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 258 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI聊天消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_message
-- ----------------------------
INSERT INTO `ai_message` VALUES (24, 10, '帮我推荐一款手机', NULL, 'user', '2026-06-16 23:35:12');
INSERT INTO `ai_message` VALUES (25, 10, '', NULL, 'assistant', '2026-06-16 23:35:18');
INSERT INTO `ai_message` VALUES (26, 11, '帮我推荐一款手机', NULL, 'user', '2026-06-16 23:39:23');
INSERT INTO `ai_message` VALUES (27, 11, '\n\n当然可以！请告诉我您对手机的需求和预算，我会根据您的需求推荐适合的机型。', NULL, 'assistant', '2026-06-16 23:39:28');
INSERT INTO `ai_message` VALUES (28, 12, '帮我推荐一款手机', NULL, 'user', '2026-06-16 23:50:59');
INSERT INTO `ai_message` VALUES (29, 12, '\n\n您好！建议您根据需求选择合适的手机。如果您需要一台性能强劲、续航能力好的旗舰机型，可以考虑推荐华为Nova 7 Pro或小米红米Note系列，它们在市场上口碑较好，功能配置也很全面。如果您的预算有限，还可以看看华为畅享系列或其他性价比高的选择。希望可以帮助到您！', NULL, 'assistant', '2026-06-16 23:51:13');
INSERT INTO `ai_message` VALUES (30, 13, '帮我推荐一款手机', NULL, 'user', '2026-06-16 23:54:56');
INSERT INTO `ai_message` VALUES (31, 13, '\n\n根据你的需求，我可以为你推荐几款受欢迎的手机：\n\n1. **小米12**：延续了小米一贯的性价比优势，性能强劲，价格实惠。\n\n2. **华为Nova系列**：设计时尚，性能稳定，适合追求高品质屏幕和长续航的用户。\n\n3. **OPPO Find X5 Pro**：适合商务人士，提供强大的专业功能。\n\n4. **vivo Y70**：游戏手机首选，性能强劲，音质出色。\n\n5. **三星Galaxy S23 Ultra**：作为旗舰机型，性能和配置都很全面。\n\n如果你有具体的预算或需求，可以告诉我，我会为你推荐最适合的选项！', NULL, 'assistant', '2026-06-16 23:55:05');
INSERT INTO `ai_message` VALUES (32, 13, '1000块', NULL, 'user', '2026-06-16 23:55:23');
INSERT INTO `ai_message` VALUES (33, 13, '\n\n预算在1000元左右的话，以下几款手机都是不错的选择：\n\n1. **小米12**  \n   性价比高，性能强劲，适合日常使用和拍照。\n\n2. **华为Nova系列**  \n   设计时尚，续航和性能也很出色，性价比很高。\n\n3. **OPPO Find X5 Pro**  \n   商务旗舰机型，设计优雅且功能强大。\n\n可以根据你的具体需求（如屏幕、续航、游戏体验等）选择适合的款式。', NULL, 'assistant', '2026-06-16 23:55:31');
INSERT INTO `ai_message` VALUES (34, 13, '200块呢', NULL, 'user', '2026-06-16 23:55:44');
INSERT INTO `ai_message` VALUES (35, 13, '\n\n200元以下的手机在市场上确实不多见，很多品牌在200元左右的产品很少。不过，如果你预算非常有限，我可以推荐一些性价比极高的入门级手机：\n\n1. **OPPO的小蓝1（Blue 1）**  \n   这款手机性价比极高，外观时尚，性能足够日常使用和拍照需求。\n\n2. **华为畅享系列**  \n   畅享系列的性价比很高，适合预算有限但追求品质的年轻人。\n\n3. **其他品牌的入门机**  \n   如果预算允许，可以考虑一些 older model 或者 older series 的手机，比如vivo、荣耀等品牌的产品。\n\n不过请注意，200元以下的手机可能在性能和功能上有所妥协，建议选择一款外观漂亮、性价比高且符合日常需求的机型。如果价格有误，请核实一下预算是否合理。', NULL, 'assistant', '2026-06-16 23:55:55');
INSERT INTO `ai_message` VALUES (36, 13, '你好', NULL, 'user', '2026-06-18 15:37:07');
INSERT INTO `ai_message` VALUES (37, 13, '\n\n你好！如果你预算有限（200元以下），目前市场上确实有些性价比极高的入门级手机可以选择。例如：\n\n1. **OPPO 小蓝 1**  \n   这款手机性价比极高，外观时尚，性能足够日常使用和拍照需求。\n\n2. **华为畅享系列**  \n   畅享系列的性价比很高，适合预算有限但追求品质的年轻人。\n\n3. **其他品牌的入门机**  \n   如果预算允许，可以考虑一些 older model 或者 older series 的手机，比如vivo、荣耀等品牌的产品。\n\n如果价格有误，请核实一下预算是否合理。如果你有其他需求或需要进一步对比，我很乐意为你推荐！', NULL, 'assistant', '2026-06-18 15:37:32');
INSERT INTO `ai_message` VALUES (38, 13, '你好', NULL, 'user', '2026-06-19 01:46:42');
INSERT INTO `ai_message` VALUES (39, 13, '\n\n你好！如果预算严格控制在200元以下，目前市场上确实很难找到高性价比的手机，但有一些入门级机型可以考虑：\n\n1. **OPPO 小蓝 1（Blue 1）**  \n   这款手机性价比极高，外观时尚，性能足够日常使用和拍照需求。\n\n2. **华为畅享系列**  \n   畅享系列的性价比很高，适合预算有限但追求品质的年轻人。\n\n3. **其他品牌的入门机**  \n   如果预算允许，可以考虑一些 older model 或者 older series 的手机，比如vivo、荣耀等品牌的产品。\n\n如果价格有误，请核实一下预算是否合理。如果你有其他需求或需要进一步对比，我很乐意为你推荐！', NULL, 'assistant', '2026-06-19 01:47:01');
INSERT INTO `ai_message` VALUES (40, 14, '这个商品有优惠吗', NULL, 'user', '2026-06-19 18:53:51');
INSERT INTO `ai_message` VALUES (41, 14, '\n\n请提供具体的商品名称和价格，以便我能更好地为您提供相关信息。', NULL, 'assistant', '2026-06-19 18:54:12');
INSERT INTO `ai_message` VALUES (42, 15, '你好', NULL, 'user', '2026-06-19 18:56:34');
INSERT INTO `ai_message` VALUES (43, 15, '\n\n你好！很高兴为你提供帮助，请问有什么我可以为你推荐的吗？', NULL, 'assistant', '2026-06-19 18:56:38');
INSERT INTO `ai_message` VALUES (44, 15, '你好', NULL, 'user', '2026-06-20 21:07:06');
INSERT INTO `ai_message` VALUES (45, 15, '\n\n你好！有什么我可以帮你的吗？', NULL, 'assistant', '2026-06-20 21:07:21');
INSERT INTO `ai_message` VALUES (46, 15, '推荐手机', NULL, 'user', '2026-06-20 21:07:33');
INSERT INTO `ai_message` VALUES (47, 15, '\n\n当然可以！不过你需要告诉我你的预算、需求以及使用场景（如日常使用、拍照、娱乐等），这样我可以为你推荐合适的手机。', NULL, 'assistant', '2026-06-20 21:07:39');
INSERT INTO `ai_message` VALUES (48, 15, '直接推荐', NULL, 'user', '2026-06-20 21:08:18');
INSERT INTO `ai_message` VALUES (49, 15, '\n\n当然可以！请告诉我你的预算、需求以及使用场景（如日常使用、拍照、娱乐等），我可以为你推荐合适的手机。', NULL, 'assistant', '2026-06-20 21:08:24');
INSERT INTO `ai_message` VALUES (50, 15, '这是什么', '/api/upload/ai_chat/58741520-f6c0-4370-8f7a-0733226a9c55.jpg', 'user', '2026-06-23 01:07:52');
INSERT INTO `ai_message` VALUES (51, 15, '感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 01:08:01');
INSERT INTO `ai_message` VALUES (52, 15, '这是什么', '/api/upload/ai_chat/d4d37879-2d47-45ee-b2c8-06bb8ce69b9d.jpg', 'user', '2026-06-23 01:08:23');
INSERT INTO `ai_message` VALUES (53, 15, '感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 01:08:23');
INSERT INTO `ai_message` VALUES (54, 15, '你好‘', NULL, 'user', '2026-06-23 01:08:27');
INSERT INTO `ai_message` VALUES (55, 15, '\n\n你好！感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 01:08:34');
INSERT INTO `ai_message` VALUES (56, 15, '推荐手机', NULL, 'user', '2026-06-23 01:08:47');
INSERT INTO `ai_message` VALUES (57, 15, '推荐手机', NULL, 'user', '2026-06-23 01:08:53');
INSERT INTO `ai_message` VALUES (58, 15, '\n\n当然可以！以下是几款不同价位和功能特点的手机推荐：\n\n1. **预算5000元以下**：  \n   - iPhone SE（支持快充）  \n   - 手机品牌：苹果；价格区间：4k-6k  \n\n2. **预算5000-8000元**：  \n   - Redmi Note 13 Pro（性能强劲，续航出色）  \n   - 手机品牌：Redmi；价格区间：5k-8k  \n\n3. **预算8000元以上**：  \n   - iPhone 15系列（高端选择）  \n   - 手机品牌：苹果；价格区间：8k+  \n\n如果你有特定需求，比如拍照、续航或性能，可以告诉我，我可以进一步推荐！', NULL, 'assistant', '2026-06-23 01:08:54');
INSERT INTO `ai_message` VALUES (59, 15, '\n\n当然可以！请告诉我您的预算、需求以及使用场景（如日常使用、拍照、娱乐等），我可以为您推荐合适的手机。', NULL, 'assistant', '2026-06-23 01:08:59');
INSERT INTO `ai_message` VALUES (60, 15, '随便推荐', NULL, 'user', '2026-06-23 01:09:23');
INSERT INTO `ai_message` VALUES (61, 15, '\n\n当然可以！以下是一些随便推荐的手机款式：\n\n1. ** budget friendly（适合预算有限）**：  \n   - iPhone SE 或者 vivo Y50  \n   - 这款手机性价比高，性能和续航都很不错。\n\n2. **拍照党的心头好**：  \n   - OPPO Find X5 Pro（支持4800万像素）  \n   - 如果你喜欢大屏幕和高分辨率的拍照效果，这是一款不错的选择。\n\n3. **时尚设计与实用性并重**：  \n   - 荣耀 Magic 4 Pro 或者华为Nova 7 SE  \n   - 这两款手机不仅外观美观，性能也很强大。\n\n如果需要更具体的方向或功能（如游戏、拍照、续航等），可以告诉我，我会为您推荐更贴合需求的手机！', NULL, 'assistant', '2026-06-23 01:09:32');
INSERT INTO `ai_message` VALUES (62, 15, '推荐手机', NULL, 'user', '2026-06-23 01:23:10');
INSERT INTO `ai_message` VALUES (63, 15, '\n\n当然可以！请告诉我您的预算、需求以及使用场景（如日常使用、拍照、娱乐等），我可以为您推荐合适的手机。', NULL, 'assistant', '2026-06-23 01:23:22');
INSERT INTO `ai_message` VALUES (64, 19, '这是什么', '/api/upload/ai_chat/e2e56ebc-8ee7-4d0a-bf8e-28bdbd4c14d1.jpg', 'user', '2026-06-23 11:21:18');
INSERT INTO `ai_message` VALUES (65, 19, '感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 11:21:28');
INSERT INTO `ai_message` VALUES (66, 19, '这是什么', '/api/upload/ai_chat/14ba3c30-ade5-4328-9150-e4fa1faaea48.jpg', 'user', '2026-06-23 11:21:34');
INSERT INTO `ai_message` VALUES (67, 19, '感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 11:21:34');
INSERT INTO `ai_message` VALUES (68, 20, '帮我推荐一款手机', NULL, 'user', '2026-06-23 11:25:05');
INSERT INTO `ai_message` VALUES (69, 20, '\n\n您好！很高兴为您推荐一款手机。根据您的需求，我为您推荐几款热门机型：\n\n1. **iPhone（比如iPhone 14 Pro）**：以其卓越的屏幕显示效果和强大的拍照功能著称，适合注重品质和设计的用户。\n\n2. **vivo Y70**：搭载最新的天玑芯片，运行流畅，外观时尚，续航表现也很好。\n\n3. **华为mate40 pro**：配备先进的徕卡摄像头，性能强大，适合追求高拍效果的用户。\n\n如果您有更多具体需求（如价格范围、屏幕大小等），可以告诉我，我会为您推荐更合适的产品哦！', NULL, 'assistant', '2026-06-23 11:25:14');
INSERT INTO `ai_message` VALUES (70, 20, '这是什么', '/api/upload/ai_chat/8f4a3f14-7e50-4e52-98a9-40018ea7f013.jpg', 'user', '2026-06-23 11:25:29');
INSERT INTO `ai_message` VALUES (71, 20, '感谢您的咨询！我是智能购物助手，可以帮您推荐商品、查询订单、了解优惠活动。请告诉我您需要什么帮助？', NULL, 'assistant', '2026-06-23 11:25:29');
INSERT INTO `ai_message` VALUES (72, 20, '你好', NULL, 'user', '2026-06-23 11:37:04');
INSERT INTO `ai_message` VALUES (73, 20, '\n\n您好！如果您有具体的预算、屏幕尺寸或其他需求，可以告诉我，我会为您推荐合适的手机型号哦！', NULL, 'assistant', '2026-06-23 11:37:17');
INSERT INTO `ai_message` VALUES (74, 20, '现在几点了', NULL, 'user', '2026-06-23 11:37:30');
INSERT INTO `ai_message` VALUES (75, 20, '\n\n您好！请您告诉我您所在的地点或使用的时区，这样我可以为您准确提供当前的时间信息。', NULL, 'assistant', '2026-06-23 11:37:32');
INSERT INTO `ai_message` VALUES (76, 20, '北京时间', NULL, 'user', '2026-06-23 11:37:38');
INSERT INTO `ai_message` VALUES (77, 20, '\n\n您好！请告诉我您需要了解当前的北京时间是几点，我会为您准确提供时间信息。', NULL, 'assistant', '2026-06-23 11:37:42');
INSERT INTO `ai_message` VALUES (78, 20, '北京时间几点了', NULL, 'user', '2026-06-23 11:38:04');
INSERT INTO `ai_message` VALUES (79, 20, '\n\n您好！请您告诉我您需要了解当前的北京时间是几点，我会为您准确提供时间信息。', NULL, 'assistant', '2026-06-23 11:38:07');
INSERT INTO `ai_message` VALUES (80, 20, '需要了解当前的北京时间是几点', NULL, 'user', '2026-06-23 11:38:24');
INSERT INTO `ai_message` VALUES (81, 20, '\n\n您好！请您告诉我您需要了解当前的北京时间是几点，我会为您准确提供时间信息。', NULL, 'assistant', '2026-06-23 11:38:29');
INSERT INTO `ai_message` VALUES (82, 20, '我问你现在是几点了', NULL, 'user', '2026-06-23 11:38:43');
INSERT INTO `ai_message` VALUES (83, 20, '\n\n您好！请您告诉我您所在的地点或使用的时区，这样我可以为您准确提供当前的时间信息。', NULL, 'assistant', '2026-06-23 11:38:47');
INSERT INTO `ai_message` VALUES (84, 20, '推荐手机', NULL, 'user', '2026-06-23 11:39:26');
INSERT INTO `ai_message` VALUES (85, 20, '\n\n您好！根据您的时间，现在是 **北京时间 [当前时间]**。如果您有其他问题或需要帮助，请随时告诉我！', NULL, 'assistant', '2026-06-23 11:39:33');
INSERT INTO `ai_message` VALUES (86, 20, '推荐手机', NULL, 'user', '2026-06-23 11:39:43');
INSERT INTO `ai_message` VALUES (87, 20, '\n\n您好！请您告诉我您需要什么样的手机（比如预算、功能需求等），我会为您推荐适合的机型。', NULL, 'assistant', '2026-06-23 11:39:46');
INSERT INTO `ai_message` VALUES (88, 20, '随便', NULL, 'user', '2026-06-23 11:39:50');
INSERT INTO `ai_message` VALUES (89, 20, '\n\n您好！请您告诉我您需要什么样的手机（比如预算、功能需求等），我会为您推荐适合的机型。', NULL, 'assistant', '2026-06-23 11:39:55');
INSERT INTO `ai_message` VALUES (90, 21, '你好', NULL, 'user', '2026-06-23 11:48:18');
INSERT INTO `ai_message` VALUES (91, 21, '您好！我是智能购物助手，可以帮您推荐商品、解答购物问题，请问有什么可以帮您的？', NULL, 'assistant', '2026-06-23 11:48:18');
INSERT INTO `ai_message` VALUES (92, 21, '推荐手机', NULL, 'user', '2026-06-23 11:48:24');
INSERT INTO `ai_message` VALUES (93, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 11:48:25');
INSERT INTO `ai_message` VALUES (94, 21, '随便推荐', NULL, 'user', '2026-06-23 11:48:35');
INSERT INTO `ai_message` VALUES (95, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 11:48:36');
INSERT INTO `ai_message` VALUES (96, 21, '推荐手机', NULL, 'user', '2026-06-23 11:49:43');
INSERT INTO `ai_message` VALUES (97, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 11:49:43');
INSERT INTO `ai_message` VALUES (98, 21, '推荐手机', NULL, 'user', '2026-06-23 11:51:43');
INSERT INTO `ai_message` VALUES (99, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 11:51:45');
INSERT INTO `ai_message` VALUES (100, 21, '推荐手机', NULL, 'user', '2026-06-23 11:54:47');
INSERT INTO `ai_message` VALUES (101, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 11:54:47');
INSERT INTO `ai_message` VALUES (102, 21, '你好', NULL, 'user', '2026-06-23 12:31:08');
INSERT INTO `ai_message` VALUES (103, 21, '您好！我是智能购物助手，可以帮您推荐商品、解答购物问题，请问有什么可以帮您的？', NULL, 'assistant', '2026-06-23 12:31:09');
INSERT INTO `ai_message` VALUES (104, 21, '推荐手机', NULL, 'user', '2026-06-23 12:31:16');
INSERT INTO `ai_message` VALUES (105, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:31:16');
INSERT INTO `ai_message` VALUES (106, 21, '推荐手机', NULL, 'user', '2026-06-23 12:43:48');
INSERT INTO `ai_message` VALUES (107, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:43:48');
INSERT INTO `ai_message` VALUES (108, 21, '推荐手机', NULL, 'user', '2026-06-23 12:50:30');
INSERT INTO `ai_message` VALUES (109, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:50:30');
INSERT INTO `ai_message` VALUES (110, 21, '推荐手机', NULL, 'user', '2026-06-23 12:53:27');
INSERT INTO `ai_message` VALUES (111, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:53:30');
INSERT INTO `ai_message` VALUES (112, 21, '顺便推荐', NULL, 'user', '2026-06-23 12:53:50');
INSERT INTO `ai_message` VALUES (113, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:53:53');
INSERT INTO `ai_message` VALUES (114, 21, '推荐手机', NULL, 'user', '2026-06-23 12:54:14');
INSERT INTO `ai_message` VALUES (115, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 12:54:16');
INSERT INTO `ai_message` VALUES (116, 21, '推荐手机', NULL, 'user', '2026-06-23 13:00:28');
INSERT INTO `ai_message` VALUES (117, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 13:00:32');
INSERT INTO `ai_message` VALUES (118, 21, '随便推荐一下', NULL, 'user', '2026-06-23 13:01:14');
INSERT INTO `ai_message` VALUES (119, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 13:01:19');
INSERT INTO `ai_message` VALUES (120, 21, '随便推荐一下手机', NULL, 'user', '2026-06-23 13:03:45');
INSERT INTO `ai_message` VALUES (121, 21, '根据您的需求，我推荐您浏览商城的热销榜单，那里有最新的热门商品！或者告诉我您想买什么类型的商品，我帮您筛选。', NULL, 'assistant', '2026-06-23 13:03:51');
INSERT INTO `ai_message` VALUES (122, 21, '随便推荐一下手机', NULL, 'user', '2026-06-23 13:07:22');
INSERT INTO `ai_message` VALUES (123, 21, '随便推荐一下手机', NULL, 'user', '2026-06-23 13:11:06');
INSERT INTO `ai_message` VALUES (124, 21, '我们注意到用户多次询问“推荐手机”，但之前的回复都是通用模板，没有给出具体推荐。需要改进。用户说“随便推荐一下手机”，应该给出一些具体的热门手机型号，而不是重复通用回答。作为智能购物助手，应该提供实际建议。可以推荐几款不同价位的手机，比如iPhone 15、小米14、一加12等，并询问用户预算或偏好。好的！根据近期热门机型，我为您推荐几款高性价比手机：\n\n1. **iPhone 16 Pro**（旗舰性能，拍照/系统流畅）\n2. **小米14 Ultra**（徕卡影像，续航强劲）\n3. **一加12**（游戏性能突出，屏幕护眼）\n4. **Redmi K70**（中端性价比之选，适合日常使用）\n\n您更看重拍照、游戏、还是续航？我可以进一步细化推荐～', NULL, 'assistant', '2026-06-23 13:11:09');
INSERT INTO `ai_message` VALUES (125, 20, '随便', NULL, 'user', '2026-06-23 13:11:21');
INSERT INTO `ai_message` VALUES (126, 20, '好的，用户问“随便”。这看起来是在回应我刚才问的具体需求，比如预算或者时间地点。用户可能觉得我上次问得太具体了，或者他/她目前没有明确的想法，就想随便聊聊。\n\n嗯，我需要判断用户的真实意图。用户可能有点不耐烦，或者只是随口一说。作为智能购物助手，我的核心任务是帮助用户解决问题，不能真的“随便”应付。我应该引导用户回到正轨，主动提供一些易懂、友好的选择，而不是纠结于他/她没有给出具体信息。\n\n考虑到对话历史，用户之前问过手机推荐，也问过时间。现在说“随便”，深层需求可能是想让我推荐一些热门的、不用费心挑选的东西，或者他/她其实想说“我也不知道要什么，你看着办吧”。我不能真的去查时间，因为用户上次问时间时就有点不配合（没给地点就让我说北京时间，我说要地点他/她又说“随便”），所以最好直接忽略时间相关的问题，把话题拉回到购物帮助这个核心功能上。\n\n所以，我应该用轻松、友好的语气回应，提供几个清晰的选项让用户选择，比如让他/她选个手机类型或价格范围，这样既能满足用户“随便”的模糊需求，又能推进到有效的购物咨询环节。哈哈，好的！如果您暂时没有具体想法，可以试试告诉我您的预算或用途（比如日常使用、拍照、打游戏等），我可以随机推荐几款高性价比手机给您参考哦！😊', NULL, 'assistant', '2026-06-23 13:11:26');
INSERT INTO `ai_message` VALUES (127, 20, '现在几点了', NULL, 'user', '2026-06-23 13:11:36');
INSERT INTO `ai_message` VALUES (128, 20, '我们注意到用户再次问“现在几点了”。之前对话中用户问过北京时间，我要求提供地点，用户说“随便”，然后我转移话题到购物推荐。现在用户又重复问时间，可能用户确实需要知道当前时间，或者是在测试我。作为购物助手，我没有实时时间获取功能，只能根据用户要求提供一般性回答。但用户明确问时间，我需要诚实地告知无法提供，并引导回购物话题。简洁回复即可。抱歉，我无法获取实时时间。建议您查看手机或电脑上的时钟哦！😊 如果需要购物帮助，随时可以告诉我。', NULL, 'assistant', '2026-06-23 13:11:38');
INSERT INTO `ai_message` VALUES (129, 22, '有什么优惠活动', NULL, 'user', '2026-06-23 13:16:21');
INSERT INTO `ai_message` VALUES (130, 22, '我们注意到用户询问“有什么优惠活动”，但当前没有提供具体商品或场景。需要先给出一般性建议，并引导用户提供更多信息。回复要简洁友好，不超过200字。当前没有具体活动信息，建议您关注我的推荐商品页面或订阅通知。如果您有想买的品类（如电子产品、服装、日用品等），可以告诉我，我会帮您查找近期优惠和性价比高的商品哦！', NULL, 'assistant', '2026-06-23 13:16:22');
INSERT INTO `ai_message` VALUES (131, 22, '这是什么', '/api/upload/ai_chat/b3b829f0-effa-403a-bf5e-df0eeac43bee.jpg', 'user', '2026-06-23 13:16:56');
INSERT INTO `ai_message` VALUES (132, 22, '这是什么', '/api/upload/ai_chat/3729419c-2192-4435-848e-aea0640b477a.jpg', 'user', '2026-06-23 13:46:08');
INSERT INTO `ai_message` VALUES (133, 22, '我们注意到用户重复提问“有什么优惠活动”，但这次用户说“这是什么”，可能是指之前的回复内容。需要解释之前的回复是通用模板，并再次引导用户提供具体需求。保持友好简洁。抱歉，刚才的回复是通用模板，想确认您想问的是“有什么优惠活动”但未指定品类。如果您有想买的商品类型（比如电子产品、服装、日用品等），可以告诉我，我会帮您查找当前平台的优惠和划算选择。或者您想了解某个具体商品的活动？直接说就好，我帮您查一查～', NULL, 'assistant', '2026-06-23 13:46:11');
INSERT INTO `ai_message` VALUES (134, 22, '这张图片是什么', '/api/upload/ai_chat/9a3c63ee-e5c1-472c-9c8b-67f94d0bc484.jpg', 'user', '2026-06-23 13:46:27');
INSERT INTO `ai_message` VALUES (135, 22, '我们注意到用户说“这张图片是什么”，但对话中没有图片。可能是用户忘记上传了，或者误以为之前有图片。需要友好地提醒用户上传图片或描述图片内容。但用户之前问过优惠活动，可能是在问某张商品图片？回复要简洁。看起来您想了解某张图片的内容，但当前对话中没有上传图片哦～请重新发送图片或简单描述一下图片（比如是商品截图、活动海报等），我会帮您识别并解答相关问题。如果有购物相关的疑问，也欢迎随时问我～', NULL, 'assistant', '2026-06-23 13:46:29');
INSERT INTO `ai_message` VALUES (136, 22, '这是什么图片', '/api/upload/ai_chat/87139c62-b027-44e0-b047-b9fa51afbd71.jpg', 'user', '2026-06-23 14:01:57');
INSERT INTO `ai_message` VALUES (137, 22, '我们注意到用户再次询问“这是什么图片”，但对话中仍没有图片。可能是用户误以为之前有图，或者网络问题。需要保持耐心，提醒用户重新上传图片或提供描述。同时保持友好简洁。您提到的图片似乎没有成功发送到对话中哦～请重新上传图片，或简单描述一下图片内容（比如商品截图、活动海报等），我会帮您识别并解答相关问题。如果是想找优惠商品，也欢迎告诉我具体品类，我来帮您查找！', NULL, 'assistant', '2026-06-23 14:02:00');
INSERT INTO `ai_message` VALUES (138, 22, '这是什么图片', '/api/upload/ai_chat/ce1b0932-1144-405e-8d39-5bdb92188f24.jpg', 'user', '2026-06-24 21:46:29');
INSERT INTO `ai_message` VALUES (139, 22, '您好～目前对话中还没有收到任何图片哦 😊  \n如果您想让我帮您识别一张图片（比如商品图、优惠海报、二维码等），请直接点击输入框旁的「📎」图标上传图片，我会立刻帮您查看内容、识别商品或解读信息！  \n\n也欢迎您告诉我：  \n✅ 想买什么类型的商品？  \n✅ 关注哪个平台（淘宝、京东、拼多多等）？  \n✅ 或有具体需求（如“学生党平价耳机”“618大促推荐”）？  \n\n我随时为您效劳～ 🌟', NULL, 'assistant', '2026-06-24 21:46:33');
INSERT INTO `ai_message` VALUES (140, 22, '这是什么图片', '/api/upload/ai_chat/f6a269c1-e249-410e-a300-36f52cbb8eb2.jpg', 'user', '2026-06-24 21:55:22');
INSERT INTO `ai_message` VALUES (141, 22, '这是什么', '/api/upload/ai_chat/2034fb97-6199-4eab-821d-d4124f5652dd.jpg', 'user', '2026-06-24 21:56:27');
INSERT INTO `ai_message` VALUES (142, 23, '这是什么', '/api/upload/ai_chat/9d66bb58-f058-40e5-bb21-d7f53cffa6c6.jpg', 'user', '2026-06-24 21:57:51');
INSERT INTO `ai_message` VALUES (143, 24, '你好', NULL, 'user', '2026-06-24 22:31:49');
INSERT INTO `ai_message` VALUES (144, 24, '你好！😊 很高兴为你服务～  \n需要帮忙挑选商品、比较款式、了解优惠，还是有其他购物问题？随时告诉我哦！', NULL, 'assistant', '2026-06-24 22:31:49');
INSERT INTO `ai_message` VALUES (145, 24, '你好', NULL, 'user', '2026-06-24 22:31:59');
INSERT INTO `ai_message` VALUES (146, 24, '你好！😊 看来你可能在测试或想确认我在～  \n放心，我一直在这儿，随时准备帮你：  \n✅ 推荐高性价比好物  \n✅ 解答商品参数/尺码/售后问题  \n✅ 分享优惠信息或选购技巧  \n需要什么，直接说吧～ 🌟', NULL, 'assistant', '2026-06-24 22:31:59');
INSERT INTO `ai_message` VALUES (147, 24, '你好', NULL, 'user', '2026-06-24 23:02:09');
INSERT INTO `ai_message` VALUES (148, 24, '[][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][{\"text\":\"你好呀！\"}][{\"text\":\"👋 看到\"}][{\"text\":\"我们连续打招呼啦\"}][{\"text\":\"～是不是还没\"}][{\"text\":\"想好要问\"}][{\"text\":\"什么？没关系\"}][{\"text\":\"，慢慢来！\"}][{\"text\":\"  \\n只要\"}][{\"text\":\"告诉我你想买的品类\"}][{\"text\":\"、\"}][{\"text\":\"预算或使用场景（\"}][{\"text\":\"比如“\"}][{\"text\":\"500元\"}][{\"text\":\"内\"}][{\"text\":\"送男友的蓝牙耳机\"}][{\"text\":\"”或\"}][{\"text\":\"“适合租房的小型\"}][{\"text\":\"洗衣机\"}][{\"text\":\"”），我马上\"}][{\"text\":\"为你精准推荐并\"}][{\"text\":\"对比优劣。随时\"}][{\"text\":\"等你开口哦！\"}][{\"text\":\"✨\"}]', NULL, 'assistant', '2026-06-24 23:02:09');
INSERT INTO `ai_message` VALUES (149, 24, '你好', NULL, 'user', '2026-06-24 23:07:52');
INSERT INTO `ai_message` VALUES (150, 24, '你好呀！😊 看来你还没想好具体需求～没关系，随时告诉我你想买的品类、预算或使用场景，我会立刻为你推荐高性价比好物，或解答比价、参数、售后等问题。期待为你效劳！🛍️✨', NULL, 'assistant', '2026-06-24 23:07:52');
INSERT INTO `ai_message` VALUES (151, 25, '帮我推荐一款手机', NULL, 'user', '2026-06-24 23:08:20');
INSERT INTO `ai_message` VALUES (152, 25, '你好！为精准推荐，请告诉我你的**预算**和**主要用途**（拍照/游戏/长续航等）。先分享几款2026年口碑机型：\n🔹 **全能性价比**：OPPO Find X8s（轻薄直屏+哈苏影像，约2799元）\n🔹 **电竞长续航**：iQOO Z11 Turbo（骁龙8+7600mAh大电池，约2899元）\n🔹 **影像旗舰**：vivo X200 Pro（蔡司长焦+天玑9400，约4569元）\n回复具体需求，我马上为你定制方案！😊', NULL, 'assistant', '2026-06-24 23:08:20');
INSERT INTO `ai_message` VALUES (153, 26, '你好', NULL, 'user', '2026-06-24 23:08:48');
INSERT INTO `ai_message` VALUES (154, 26, '你好！我是你的智能购物助手，随时为你挑选好物、解答购物疑问。无论是数码家电、服饰美妆，还是日常家居，只需告诉我你的需求、预算或使用场景，我就能为你推荐高性价比的选项。今天想看点什么呢？😊', NULL, 'assistant', '2026-06-24 23:08:48');
INSERT INTO `ai_message` VALUES (155, 26, '这是什么', '/api/upload/ai_chat/f0abcf36-77fe-448d-b2a9-8fd4eebedfce.jpg', 'user', '2026-06-24 23:09:18');
INSERT INTO `ai_message` VALUES (156, 26, '这是一款华硕ROG（玩家国度）机械游戏键盘。它带有炫酷的RGB背光，专为电竞设计，通常搭载机械轴体，触发快、手感好，兼顾游戏与日常打字。如果你需要具体型号推荐、比价或购买渠道建议，告诉我你的预算和常用场景（如FPS游戏、办公等），', NULL, 'assistant', '2026-06-24 23:09:18');
INSERT INTO `ai_message` VALUES (157, 26, '帮我推荐一款手机', NULL, 'user', '2026-07-09 20:50:18');
INSERT INTO `ai_message` VALUES (158, 26, '我们需要理解用户需求：用户两次说“帮我推荐一款手机”，可能只是重复。作为购物助手，需要先询问预算、用途等关键信息。但根据规则回答要简洁友好不超过200字。所以可以给出通用建议，并引导用户提供更多信息。好的！为了给你推荐最合适的手机，先问几个小问题：预算大概多少（比如2000元内、4000元左右）？主要用途是拍照、游戏、还是日常综合使用？对品牌有偏好（如苹果、华为、小米等）吗？告诉我这些，我就能精准推荐啦~ 😊', NULL, 'assistant', '2026-07-09 20:50:23');
INSERT INTO `ai_message` VALUES (159, 26, '小米', NULL, 'user', '2026-07-09 20:50:42');
INSERT INTO `ai_message` VALUES (160, 26, '我们注意到用户只说了“小米”，可能是对之前问题的回应：表示品牌偏好小米。那么作为购物助手，应该根据小米品牌给出推荐，但还需要更多信息（预算、用途等）。然而用户只给了品牌名，可能需要进一步询问。但规则要求不超过200字，所以可以简洁地给出常见价位段的推荐，并提示用户补充细节。好的，你偏好小米品牌！目前小米旗下有多个系列：\n- **小米数字系列**（如小米14/14 Pro）：主打旗舰性能、拍照和综合体验，预算4000+。\n- **Redmi K系列**（如K70 Pro）：性价比高，适合游戏和日常，预算2000-3500。\n- **Redmi Note系列**（如Note 13 Pro）：均衡千元机，适合实用党。\n- **Civi系列**：轻薄自拍，适合女生。\n\n请告诉我：你的预算大概多少？主要用来打游戏、拍照还是日常使用？我帮你精准推荐~ 😊', NULL, 'assistant', '2026-07-09 20:50:46');
INSERT INTO `ai_message` VALUES (161, 26, '帮我推荐一款手机', NULL, 'user', '2026-07-11 15:22:11');
INSERT INTO `ai_message` VALUES (162, 26, '我们注意到用户第三次说“帮我推荐一款手机”，可能是重复、或者测试、或者不耐烦。作为购物助手，需要保持耐心。但之前已经询问过预算和用途，用户只说了“小米”。现在用户再次要求推荐，可能希望直接给答案。那么可以基于“小米”品牌，假设一个常见需求（比如2000-3000元性价比）给出推荐。或者更机智地引导用户提供关键信息。但规则要求不超过200字，且简洁友好。可以给出一个具体推荐并附上可选信息。考虑到用户可能想快速得到答案，推荐一款热门的Redmi K70（性价比高，2000-3000元档），并提示如果需求不同可调整。好的，我明白您想直接获得推荐！如果您偏好小米品牌且没有特别说明预算，我推荐 **Redmi K70**（2000-3000元档）：  \n- 搭载骁龙8 Gen2处理器，游戏和日常流畅；  \n- 2K直屏，护眼又清晰；  \n- 5000万像素主摄+OIS光学防抖，拍照够用。  \n若预算更高（4000+），可选 **小米14**（小屏旗舰、徕卡影像）；若追求极致性价比，**Redmi Note 13 Pro**（1500元档）也是好选择。  \n请告诉我您的具体预算或使用场景，我可以调整推荐~ 😊', NULL, 'assistant', '2026-07-11 15:22:16');
INSERT INTO `ai_message` VALUES (163, 26, '我的购物车里有什么', NULL, 'user', '2026-07-11 15:23:17');
INSERT INTO `ai_message` VALUES (164, 26, '我们注意到用户突然问“我的购物车里有什么”，这需要访问用户的购物车数据。作为AI助手，我无法直接访问用户的账户信息（除非在特定平台如购物网站内嵌的助手，但这里没有说明）。出于安全和隐私，不能假装知道。应该诚实告知无法查看，并提供建议。回答不超过200字。抱歉，我无法直接查看您的购物车内容，因为这是个人隐私信息，且我没有访问您账户的权限。😊  \n您可以在电商APP（如淘宝、京东、小米商城等）的“购物车”页面自行查看。如果您需要我帮您分析购物车里的商品（比如比价、评估性价比），请把商品名称或链接告诉我，我会尽力帮忙！', NULL, 'assistant', '2026-07-11 15:23:19');
INSERT INTO `ai_message` VALUES (225, 32, '帮我查一下购物车', NULL, 'user', '2026-08-11 18:42:20');
INSERT INTO `ai_message` VALUES (226, 32, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:42:20');
INSERT INTO `ai_message` VALUES (227, 32, '', NULL, 'assistant', '2026-08-11 18:42:20');
INSERT INTO `ai_message` VALUES (228, 32, '帮我查一下购物车', NULL, 'user', '2026-08-11 18:42:21');
INSERT INTO `ai_message` VALUES (229, 32, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:42:21');
INSERT INTO `ai_message` VALUES (230, 32, '您的购物车当前共有4种商品（共4件），且均已勾选，明细如下：\n- 施华洛世奇项链：¥899.00\n- 三合一快充数据线：¥35.00\n- 华为Mate60 Pro：¥6299.00\n- 小米14 Ultra：¥5999.00\n\n商品合计：¥13,232.00。如需修改数量、取消勾选或直接结算，请随时告诉我！', NULL, 'assistant', '2026-08-11 18:42:21');
INSERT INTO `ai_message` VALUES (233, 36, '购物车有什么', NULL, 'user', '2026-08-11 18:47:16');
INSERT INTO `ai_message` VALUES (234, 36, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:47:16');
INSERT INTO `ai_message` VALUES (235, 36, '您好！您的购物车里目前有4件商品，且都已勾选，分别是：\n- 施华洛世奇项链（¥899.00）\n- 三合一快充数据线（¥35.00）\n- 华为Mate60 Pro（¥6299.00）\n- 小米14 Ultra（¥5999.00）\n\n请问需要调整数量、删除商品，还是直接结算呢？随时为您效劳！', NULL, 'assistant', '2026-08-11 18:47:16');
INSERT INTO `ai_message` VALUES (236, 36, '购物车有什么', NULL, 'user', '2026-08-11 18:48:38');
INSERT INTO `ai_message` VALUES (237, 36, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:48:38');
INSERT INTO `ai_message` VALUES (238, 36, '您的购物车当前共有4种商品（共4件），且均已勾选：\n1. 施华洛世奇项链（¥899）｜美妆时尚旗舰店\n2. 三合一快充数据线（¥35）｜王五数码精品店\n3. 华为Mate60 Pro（¥6299）｜数码官方旗舰店\n4. 小米14 Ultra（¥5999）｜优品生活百货\n合计：¥13,232。如需调整数量或去结算，请随时告诉我！', NULL, 'assistant', '2026-08-11 18:48:38');
INSERT INTO `ai_message` VALUES (239, 36, '购物车有什么', NULL, 'user', '2026-08-11 18:51:49');
INSERT INTO `ai_message` VALUES (240, 36, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:51:49');
INSERT INTO `ai_message` VALUES (241, 36, '您的购物车当前', NULL, 'assistant', '2026-08-11 18:51:49');
INSERT INTO `ai_message` VALUES (242, 36, '购物车有什么', NULL, 'user', '2026-08-11 18:52:26');
INSERT INTO `ai_message` VALUES (243, 36, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 18:52:26');
INSERT INTO `ai_message` VALUES (244, 36, '您的购物车当前共有4种商品（总计4件），且已全部勾选。明细如下：\n• 施华洛世奇项链：¥899.00 ×1\n• 三合一快充数据线：¥35.00 ×1\n• 华为Mate60 Pro：¥6299.00 ×1\n• 小米14 Ultra：¥5999.00 ×1\n\n如需调整数量、取消勾选或直接结算，请随时告诉我。', NULL, 'assistant', '2026-08-11 18:52:26');
INSERT INTO `ai_message` VALUES (245, 38, '购物车有什么', NULL, 'user', '2026-08-11 19:04:02');
INSERT INTO `ai_message` VALUES (246, 38, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 19:04:02');
INSERT INTO `ai_message` VALUES (247, 38, '您的购物车里目前有 4 件商品，且都已勾选：\n1. 施华洛世奇项链 ¥899.00\n2. 三合一快充数据线 ¥35.00\n3. 华为Mate60 Pro ¥6299.00\n4. 小米14 Ultra ¥5999.00\n\n请问需要修改数量或结算吗？', NULL, 'assistant', '2026-08-11 19:04:02');
INSERT INTO `ai_message` VALUES (248, 38, '购物车有什么', NULL, 'user', '2026-08-11 19:08:02');
INSERT INTO `ai_message` VALUES (249, 38, '', NULL, 'assistant', '2026-08-11 19:08:02');
INSERT INTO `ai_message` VALUES (250, 38, '购物车有什么', NULL, 'user', '2026-08-11 19:24:28');
INSERT INTO `ai_message` VALUES (251, 38, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n', NULL, 'system', '2026-08-11 19:24:28');
INSERT INTO `ai_message` VALUES (252, 38, '您的购物车里目前有 4 件商品，且都已勾选，分别是：\n1. 施华洛世奇项链（¥899.00）\n2. 三合一快充数据线（¥35.00）\n3. 华为Mate60 Pro（¥6299.00）\n4. 小米14 Ultra（¥5999.00）\n\n请问需要修改数量、删除商品还是直接结算？', NULL, 'assistant', '2026-08-11 19:24:28');
INSERT INTO `ai_message` VALUES (253, 31, '购物车有什么', NULL, 'user', '2026-08-12 15:00:09');
INSERT INTO `ai_message` VALUES (254, 31, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n', NULL, 'system', '2026-08-12 15:00:09');
INSERT INTO `ai_message` VALUES (255, 31, '您的购物车里目前有 4 件商品，且均已勾选：\n\n1. **施华洛世奇项链**（美妆时尚旗舰店）- ¥899.00\n2. **小米14 Ultra**（优品生活百货）- ¥5999.00\n3. **华为Mate60 Pro**（数码官方旗舰店）- ¥6299.00\n4. **三合一快充数据线**（王五数码精品店）- ¥35.00\n\n请问您是想直接结算，还是需要修改商品数量呢？', NULL, 'assistant', '2026-08-12 15:00:09');
INSERT INTO `ai_message` VALUES (256, 38, '购物车有什么', NULL, 'user', '2026-08-17 21:46:36');
INSERT INTO `ai_message` VALUES (257, 38, '[cart_query] 成功: 购物车共 4 种商品，4 件，其中已勾选 4 种。购物车当前有 \n- 小米14 Ultra，价格：¥5999.00，数量：1，店铺：优品生活百货（已勾选）\n- 三合一快充数据线，价格：¥35.00，数量：1，店铺：王五数码精品店（已勾选）\n- 施华洛世奇项链，价格：¥899.00，数量：1，店铺：美妆时尚旗舰店（已勾选）\n- 华为Mate60 Pro，价格：¥6299.00，数量：1，店铺：数码官方旗舰店（已勾选）\n', NULL, 'system', '2026-08-17 21:46:36');
INSERT INTO `ai_message` VALUES (258, 38, '您好！您的购物车里目前有 **4件** 商品，且都已勾选，分别是：\n\n1. **小米14 Ultra** — ¥5999.00\n2. **三合一快充数据线** — ¥35.00\n3. **施华洛世奇项链** — ¥899.00\n4. **华为Mate60 Pro** — ¥6299.00\n\n请问需要帮您结算或调整商品数量吗？', NULL, 'assistant', '2026-08-17 21:46:36');
INSERT INTO `ai_message` VALUES (259, 38, '我的购物车里有什么', NULL, 'user', '2026-08-25 20:36:53');
INSERT INTO `ai_message` VALUES (260, 38, '我的购物车里有什么', NULL, 'user', '2026-08-25 20:37:05');
INSERT INTO `ai_message` VALUES (261, 38, '您好！作为智能购物助手，我目前无法直接登录您的账户或实时查看您的个人购物车数据。建议您直接打开您常用的购物平台（APP或网页），点击页面底部或右上角的**“购物车”**图标，即可查看当前已添加的商品、数量、价格及优惠状态。\n\n💡 **我可以帮您做这些：**\n- 如果您把购物车里的商品名称/链接发给我，我可以帮您**比价、查历史低价、找平替或凑单建议**\n- 需要清理过期商品、合并同类项、计算满减最优组合？告诉我清单即可\n- 想根据购物车里的商品推荐搭配或同类爆款？随时发我\n\n请随时把您的购物车清单或具体需求发给我，我会立刻为您处理！', NULL, 'assistant', '2026-08-25 20:37:05');
INSERT INTO `ai_message` VALUES (262, 38, '我的购物车里有什么', NULL, 'user', '2026-08-25 20:46:22');
INSERT INTO `ai_message` VALUES (263, 38, '[cart_query] 成功: 购物车是空的，还没有任何商品。建议用户去商城逛逛添加商品。\n', NULL, 'system', '2026-08-25 20:46:22');
INSERT INTO `ai_message` VALUES (264, 38, '亲，帮您查询了一下，目前您的购物车是空的哦，里面还没有添加任何商品。如果您有想要购买的宝贝，可以去逛逛并加入购物车；或者告诉我您想买什么类型的商品，我也可以为您推荐哦！', NULL, 'assistant', '2026-08-25 20:46:22');
INSERT INTO `ai_message` VALUES (265, 38, '我的购物车里有什么', NULL, 'user', '2026-08-25 20:46:54');
INSERT INTO `ai_message` VALUES (266, 38, '[cart_query] 成功: 购物车共 1 种商品，1 件，其中已勾选 1 种。购物车当前有 \n- 静音无线鼠标，价格：¥79.00，数量：1，店铺：王五数码精品店（已勾选）\n', NULL, 'system', '2026-08-25 20:46:54');
INSERT INTO `ai_message` VALUES (267, 38, '亲，帮您查了一下，您的购物车里目前有 **1** 件商品，详情如下：\n\n- **静音无线鼠标**（¥79.00）× 1\n  来自店铺：王五数码精品店（已勾选）\n\n请问您需要修改数量、删除商品，还是直接去结算呢？', NULL, 'assistant', '2026-08-25 20:46:54');

-- ----------------------------
-- Table structure for ai_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_session`;
CREATE TABLE `ai_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '新对话' COMMENT '会话标题',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NULL DEFAULT NULL COMMENT '最后操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_session
-- ----------------------------
INSERT INTO `ai_session` VALUES (1, 1, '华为手机咨询', '2026-06-07 13:54:05', NULL);
INSERT INTO `ai_session` VALUES (2, 2, '衣服尺码问题', '2026-06-07 13:54:05', NULL);
INSERT INTO `ai_session` VALUES (4, 4, '电饭煲怎么用', '2026-06-07 13:54:05', NULL);
INSERT INTO `ai_session` VALUES (5, 5, '坚果保质期', '2026-06-07 13:54:05', NULL);
INSERT INTO `ai_session` VALUES (36, 3, '购物车有什么', '2026-08-11 18:42:01', '2026-08-11 18:52:26');
INSERT INTO `ai_session` VALUES (38, 3, '购物车有什么', '2026-08-11 18:52:06', '2026-08-25 20:46:54');

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `num` int NOT NULL DEFAULT 1 COMMENT '选购数量',
  `checked` tinyint NOT NULL DEFAULT 1 COMMENT '0未选中 1选中',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_sku`(`user_id`, `sku_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '购物车' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart
-- ----------------------------
INSERT INTO `cart` VALUES (1, 1, 2, 3, 1, 1, '2026-06-07 13:54:05');
INSERT INTO `cart` VALUES (2, 1, 5, 5, 1, 1, '2026-06-07 13:54:05');
INSERT INTO `cart` VALUES (3, 2, 5, 5, 2, 1, '2026-06-07 13:54:05');
INSERT INTO `cart` VALUES (5, 4, 4, 1, 1, 1, '2026-06-07 13:54:05');
INSERT INTO `cart` VALUES (6, 4, 2, 2, 2, 1, '2026-06-11 23:26:01');
INSERT INTO `cart` VALUES (56, 3, 41, 34, 1, 1, '2026-06-26 21:30:21');
INSERT INTO `cart` VALUES (58, 12, 20, 58, 1, 1, '2026-07-09 20:54:10');
INSERT INTO `cart` VALUES (60, 3, 3, 37, 1, 1, '2026-07-11 18:31:41');
INSERT INTO `cart` VALUES (61, 3, 15, 50, 1, 1, '2026-07-18 19:29:16');
INSERT INTO `cart` VALUES (62, 3, 1, 1, 1, 1, '2026-08-06 18:16:18');
INSERT INTO `cart` VALUES (63, 5, 39, 31, 1, 1, '2026-08-25 20:46:39');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父分类id，0顶级',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (1, '手机数码', 0, 1);
INSERT INTO `category` VALUES (2, '服饰鞋包', 0, 2);
INSERT INTO `category` VALUES (3, '零食食品', 0, 3);
INSERT INTO `category` VALUES (4, '家用电器', 0, 4);
INSERT INTO `category` VALUES (5, '美妆护肤', 0, 5);
INSERT INTO `category` VALUES (6, '手机', 1, 1);
INSERT INTO `category` VALUES (7, '平板', 1, 2);
INSERT INTO `category` VALUES (8, '笔记本电脑', 1, 3);
INSERT INTO `category` VALUES (9, '智能手表', 1, 4);
INSERT INTO `category` VALUES (10, '耳机/音响', 1, 5);
INSERT INTO `category` VALUES (11, '男装', 2, 1);
INSERT INTO `category` VALUES (12, '女装', 2, 2);
INSERT INTO `category` VALUES (13, '运动鞋', 2, 3);
INSERT INTO `category` VALUES (14, '箱包', 2, 4);
INSERT INTO `category` VALUES (15, '配饰', 2, 5);
INSERT INTO `category` VALUES (16, '坚果炒货', 3, 1);
INSERT INTO `category` VALUES (17, '饼干糕点', 3, 2);
INSERT INTO `category` VALUES (18, '肉干肉脯', 3, 3);
INSERT INTO `category` VALUES (19, '饮料冲调', 3, 4);
INSERT INTO `category` VALUES (20, '方便速食', 3, 5);
INSERT INTO `category` VALUES (21, '厨房电器', 4, 1);
INSERT INTO `category` VALUES (22, '生活电器', 4, 2);
INSERT INTO `category` VALUES (23, '个护健康', 4, 3);
INSERT INTO `category` VALUES (24, '大家电', 4, 4);
INSERT INTO `category` VALUES (25, '小家电配件', 4, 5);
INSERT INTO `category` VALUES (26, '面部护肤', 5, 1);
INSERT INTO `category` VALUES (27, '彩妆', 5, 2);
INSERT INTO `category` VALUES (28, '香水香氛', 5, 3);
INSERT INTO `category` VALUES (29, '个护清洁', 5, 4);
INSERT INTO `category` VALUES (30, '美妆工具', 5, 5);

-- ----------------------------
-- Table structure for chat_merchant_msg
-- ----------------------------
DROP TABLE IF EXISTS `chat_merchant_msg`;
CREATE TABLE `chat_merchant_msg`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `send_type` tinyint NOT NULL COMMENT '发送者类型：1=买家，2=商家',
  `send_id` bigint NOT NULL COMMENT '发送者用户ID',
  `receive_id` bigint NOT NULL COMMENT '接收者用户ID',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型：1=文字，2=商品卡片，3=订单信息',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `extra_data` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扩展数据(JSON)',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id`) USING BTREE,
  INDEX `idx_send_id`(`send_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商家客服消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_merchant_msg
-- ----------------------------
INSERT INTO `chat_merchant_msg` VALUES (16, 5, 1, 5, 4, 1, '你好', NULL, 1, '2026-06-19 19:45:09');
INSERT INTO `chat_merchant_msg` VALUES (17, 5, 2, 3, 5, 1, '你好‘', NULL, 1, '2026-06-19 19:45:30');
INSERT INTO `chat_merchant_msg` VALUES (18, 5, 1, 5, 4, 1, 'nihao', NULL, 0, '2026-06-19 19:45:47');
INSERT INTO `chat_merchant_msg` VALUES (19, 5, 2, 3, 5, 1, 'nihao', NULL, 1, '2026-06-19 19:45:53');
INSERT INTO `chat_merchant_msg` VALUES (20, 6, 2, 3, 5, 1, 'nihao', NULL, 1, '2026-06-19 19:53:35');
INSERT INTO `chat_merchant_msg` VALUES (21, 6, 1, 5, 4, 1, 'hi', NULL, 1, '2026-06-19 20:02:07');
INSERT INTO `chat_merchant_msg` VALUES (22, 6, 2, 3, 5, 1, 'hi', NULL, 1, '2026-06-20 23:57:50');
INSERT INTO `chat_merchant_msg` VALUES (23, 6, 1, 5, 4, 1, 'hi', NULL, 1, '2026-06-20 23:59:21');
INSERT INTO `chat_merchant_msg` VALUES (24, 6, 1, 5, 4, 1, 'xx', NULL, 1, '2026-06-20 23:59:35');
INSERT INTO `chat_merchant_msg` VALUES (25, 6, 1, 5, 4, 1, '你好', NULL, 1, '2026-06-22 16:00:39');
INSERT INTO `chat_merchant_msg` VALUES (26, 6, 1, 5, 4, 1, '你好', NULL, 1, '2026-06-22 17:54:52');
INSERT INTO `chat_merchant_msg` VALUES (27, 6, 2, 3, 5, 1, '你好', NULL, 1, '2026-06-22 17:54:59');
INSERT INTO `chat_merchant_msg` VALUES (28, 6, 1, 5, 4, 1, '你好，测试消息', NULL, 1, '2026-08-11 20:18:44');
INSERT INTO `chat_merchant_msg` VALUES (29, 6, 1, 5, 4, 1, '你好', NULL, 1, '2026-08-11 20:36:16');
INSERT INTO `chat_merchant_msg` VALUES (30, 6, 1, 5, 4, 1, '你好', NULL, 1, '2026-08-11 20:36:48');
INSERT INTO `chat_merchant_msg` VALUES (31, 6, 1, 5, 4, 1, '你好', NULL, 1, '2026-08-11 21:00:06');
INSERT INTO `chat_merchant_msg` VALUES (32, 6, 2, 3, 5, 1, '你好', NULL, 0, '2026-08-11 21:00:13');

-- ----------------------------
-- Table structure for chat_merchant_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_merchant_session`;
CREATE TABLE `chat_merchant_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '买家用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家用户ID',
  `shop_id` bigint NULL DEFAULT NULL COMMENT '关联店铺ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '关联商品ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联订单号',
  `status` tinyint NULL DEFAULT 0 COMMENT '0=进行中 1=已结束',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_merchant_id`(`merchant_id`) USING BTREE,
  INDEX `idx_shop_id`(`shop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商家客服会话（买家 ↔ 商家）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_merchant_session
-- ----------------------------
INSERT INTO `chat_merchant_session` VALUES (6, 5, 4, NULL, NULL, NULL, 0, '2026-06-19 19:51:06', NULL);

-- ----------------------------
-- Table structure for chat_platform_msg
-- ----------------------------
DROP TABLE IF EXISTS `chat_platform_msg`;
CREATE TABLE `chat_platform_msg`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `send_type` tinyint NOT NULL COMMENT '发送者类型：1=买家，2=管理员',
  `send_id` bigint NOT NULL COMMENT '发送者用户ID',
  `receive_id` bigint NOT NULL COMMENT '接收者用户ID',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型：1=文字，2=商品卡片',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `extra_data` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扩展数据(JSON)',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id`) USING BTREE,
  INDEX `idx_send_id`(`send_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台客服消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_platform_msg
-- ----------------------------
INSERT INTO `chat_platform_msg` VALUES (1, 1, 1, 7, 7, 1, '你好', NULL, 1, '2026-06-18 20:56:45');
INSERT INTO `chat_platform_msg` VALUES (2, 1, 2, 6, 7, 1, '你好', NULL, 1, '2026-06-19 14:45:22');
INSERT INTO `chat_platform_msg` VALUES (3, 1, 2, 1, 7, 1, '你好', NULL, 1, '2026-06-19 17:09:49');
INSERT INTO `chat_platform_msg` VALUES (4, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-19 17:56:15');
INSERT INTO `chat_platform_msg` VALUES (5, 2, 2, 6, 5, 1, '你好', NULL, 1, '2026-06-19 17:56:20');
INSERT INTO `chat_platform_msg` VALUES (6, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-19 19:15:55');
INSERT INTO `chat_platform_msg` VALUES (7, 2, 1, 5, 6, 1, '你好’】', NULL, 1, '2026-06-19 19:29:50');
INSERT INTO `chat_platform_msg` VALUES (8, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-22 18:20:15');
INSERT INTO `chat_platform_msg` VALUES (9, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-22 19:08:12');
INSERT INTO `chat_platform_msg` VALUES (10, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-22 19:17:03');
INSERT INTO `chat_platform_msg` VALUES (11, 2, 1, 5, 6, 1, '你好', NULL, 1, '2026-06-22 19:34:05');

-- ----------------------------
-- Table structure for chat_platform_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_platform_session`;
CREATE TABLE `chat_platform_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '买家用户ID',
  `admin_id` bigint NULL DEFAULT NULL COMMENT '平台管理员用户ID',
  `status` tinyint NULL DEFAULT 0 COMMENT '0=进行中 1=已结束',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_admin_id`(`admin_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台客服会话（买家 ↔ 平台管理员）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_platform_session
-- ----------------------------
INSERT INTO `chat_platform_session` VALUES (1, 7, 7, 1, '2026-06-18 20:56:40', '2026-06-20 14:29:51');
INSERT INTO `chat_platform_session` VALUES (2, 5, 6, 0, '2026-06-19 17:55:54', '2026-06-20 14:29:48');
INSERT INTO `chat_platform_session` VALUES (3, 3, NULL, 0, '2026-06-20 13:40:10', NULL);
INSERT INTO `chat_platform_session` VALUES (4, 12, NULL, 0, '2026-07-09 20:52:00', NULL);

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_up`(`user_id`, `product_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collect
-- ----------------------------
INSERT INTO `collect` VALUES (1, 1, 1, '2026-06-07 13:54:05');
INSERT INTO `collect` VALUES (2, 1, 2, '2026-06-07 13:54:05');
INSERT INTO `collect` VALUES (3, 2, 3, '2026-06-07 13:54:05');
INSERT INTO `collect` VALUES (4, 3, 5, '2026-06-07 13:54:05');
INSERT INTO `collect` VALUES (5, 4, 4, '2026-06-07 13:54:05');

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评价内容',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评价图片(逗号分隔)',
  `score` tinyint NOT NULL DEFAULT 5 COMMENT '1~5星评分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '管理员/商家回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '0未删1已删',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常 2-隐藏',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pid`(`product_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品评价' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (1, 1, 1, 1, NULL, '手机非常好用，拍照清晰，续航强', NULL, 5, '2026-06-07 13:54:05', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (2, 2, 2, 2, NULL, '衣服质量很好，大小合适', NULL, 4, '2026-06-07 13:54:05', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (3, 3, 5, 2, NULL, '面膜补水效果很好', NULL, 5, '2026-06-07 13:54:05', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (4, 1, 2, 1, 1, 'iPhone做工精致，拍照效果一流', NULL, 5, '2026-06-14 10:00:00', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (5, 3, 16, 2, 3, '坚果挺新鲜的，好吃', NULL, 4, '2026-06-14 11:00:00', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (6, 6, 22, 2, 3, '电饭煲煮饭很香，功能齐全', 'https://picsum.photos/200/210', 5, '2026-06-14 12:00:00', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (7, 6, 28, 2, 3, '面膜补水效果不错，会回购', NULL, 4, '2026-06-14 12:30:00', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (8, 11, 35, 4, 1, '耳机音质超出预期，佩戴舒适', 'https://picsum.photos/200/213', 5, '2026-06-13 12:00:00', '感谢您的支持，祝您使用愉快！', '2026-06-13 14:00:00', 0, 1);
INSERT INTO `comment` VALUES (9, 12, 36, 4, 2, '充电宝容量大，充手机可以充4次', NULL, 4, '2026-06-13 15:00:00', NULL, NULL, 0, 1);
INSERT INTO `comment` VALUES (10, 12, 37, 4, 2, '扩展坞做工不错，但发热有点大', NULL, 3, '2026-06-13 15:01:00', NULL, NULL, 0, 1);
INSERT INTO `comment` VALUES (11, 14, 39, 4, 2, '鼠标很轻便，办公够用', NULL, 4, '2026-06-14 08:00:00', '谢谢亲的好评~', '2026-06-14 09:00:00', 0, 1);
INSERT INTO `comment` VALUES (12, 3, 5, 2, 3, '好', NULL, 5, '2026-06-20 17:25:50', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (13, 1781710682086876, 36, 4, 3, '好', '', 5, '2026-06-20 17:33:57', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (14, 1781707699162231, 42, 4, 3, NULL, '', 5, '2026-06-20 18:36:55', '', NULL, 0, 1);
INSERT INTO `comment` VALUES (15, 1781705692745192, 41, 4, 3, '好', NULL, 5, '2026-06-20 19:02:29', '好', '2026-06-20 23:57:36', 0, 1);
INSERT INTO `comment` VALUES (16, 9, 23, 2, 3, '1', '/api/upload/comment/c5c9ca4a-0bf9-45da-b63e-6ca0cc916077.jpg', 5, '2026-06-20 21:54:15', '', NULL, 0, 1);

-- ----------------------------
-- Table structure for coupon
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '券名称',
  `type` tinyint NOT NULL COMMENT '1满减2折扣',
  `min_price` decimal(10, 2) NOT NULL COMMENT '满多少可用',
  `discount` decimal(10, 2) NOT NULL COMMENT '减免金额/折扣',
  `stock` int NOT NULL DEFAULT 0 COMMENT '发放总量',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `end_time` datetime NOT NULL COMMENT '过期时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠券' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon
-- ----------------------------
INSERT INTO `coupon` VALUES (1, '满100减20', 1, 10.00, 20.00, 500, '2026-01-01 00:00:00', '2026-12-01 00:00:00', 1);
INSERT INTO `coupon` VALUES (2, '满300减50', 1, 300.00, 50.00, 300, '2026-01-01 00:00:00', '2027-01-01 00:00:00', 1);
INSERT INTO `coupon` VALUES (3, '9折优惠', 2, 50.00, 0.90, 200, '2025-01-01 00:00:00', '2025-12-31 00:00:00', 1);

-- ----------------------------
-- Table structure for flash_sale
-- ----------------------------
DROP TABLE IF EXISTS `flash_sale`;
CREATE TABLE `flash_sale`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `flash_price` decimal(10, 2) NOT NULL COMMENT '秒杀价格',
  `stock` int NOT NULL DEFAULT 0 COMMENT '秒杀库存',
  `start_time` datetime NOT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NOT NULL COMMENT '秒杀结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束',
  `limit_per_user` int NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `sign_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '动态签名密钥',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id`) USING BTREE,
  INDEX `idx_start_time`(`start_time`) USING BTREE,
  INDEX `idx_end_time`(`end_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '秒杀活动商品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flash_sale
-- ----------------------------
INSERT INTO `flash_sale` VALUES (6, 1, 99.00, 100, '2026-08-17 19:56:30', '2027-02-01 20:56:30', 1, 2, 'fix_key_1', '2026-08-17 20:56:30', '2026-08-25 17:27:41');
INSERT INTO `flash_sale` VALUES (7, 2, 199.00, 80, '2026-08-17 18:56:30', '2027-03-01 20:56:30', 1, 1, 'fix_key_2', '2026-08-17 20:56:30', '2026-08-25 17:27:46');
INSERT INTO `flash_sale` VALUES (8, 3, 49.00, 200, '2026-08-17 20:26:30', '2027-03-01 20:56:30', 1, 3, 'fix_key_3', '2026-08-17 20:56:30', '2026-08-25 17:27:51');
INSERT INTO `flash_sale` VALUES (9, 4, 299.00, 50, '2026-08-17 17:56:30', '2027-02-01 20:56:30', 1, 1, 'fix_key_4', '2026-08-17 20:56:30', '2026-08-25 17:27:56');
INSERT INTO `flash_sale` VALUES (10, 5, 79.00, 150, '2026-08-17 19:56:30', '2027-05-01 20:56:30', 1, 2, 'fix_key_5', '2026-08-17 20:56:30', '2026-08-25 17:28:04');

-- ----------------------------
-- Table structure for merchant
-- ----------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联用户',
  `merchant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商家名称',
  `license_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业执照',
  `contact` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NULL DEFAULT 0 COMMENT '0待审核 1正常 2禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商家表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant
-- ----------------------------
INSERT INTO `merchant` VALUES (1, 5, '数码科技专营店', '91350100MA12345678', '孙七', '13800138005', 1, '2026-06-07 13:54:04', 0, '通过', '2026-06-11 16:46:06');
INSERT INTO `merchant` VALUES (2, 6, '优品百货店', '91350200MA87654321', '店主', '13800138006', 1, '2026-06-07 13:54:04', 0, NULL, NULL);
INSERT INTO `merchant` VALUES (3, 8, '美妆时尚旗舰店', '91350100MA88888888', '美妆店主', '13800138008', 1, '2026-06-14 00:00:00', 1, '审核通过', '2026-06-14 00:00:00');
INSERT INTO `merchant` VALUES (4, 3, '王五数码精品店', '91350100MA99999999', '王五', '13800138003', 1, '2026-01-01 00:00:00', 1, '审核通过', '2026-01-01 00:00:00');

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `num` int NOT NULL COMMENT '购买数量',
  `price` decimal(10, 2) NOT NULL COMMENT '下单单价',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_oid`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 87 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单详情' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------
INSERT INTO `order_item` VALUES (1, 1, 1, 1, 1, 1, 6299.00);
INSERT INTO `order_item` VALUES (2, 2, 2, 3, 2, 1, 269.00);
INSERT INTO `order_item` VALUES (3, 3, 5, 5, 2, 1, 69.90);
INSERT INTO `order_item` VALUES (4, 3, 16, 3, 2, 2, 79.90);
INSERT INTO `order_item` VALUES (5, 6, 22, 3, 2, 1, 189.00);
INSERT INTO `order_item` VALUES (6, 6, 28, 5, 2, 3, 69.90);
INSERT INTO `order_item` VALUES (7, 7, 8, 6, 2, 1, 269.00);
INSERT INTO `order_item` VALUES (8, 8, 22, 15, 2, 1, 189.00);
INSERT INTO `order_item` VALUES (9, 9, 23, 16, 2, 1, 299.00);
INSERT INTO `order_item` VALUES (10, 10, 16, 14, 2, 1, 79.90);
INSERT INTO `order_item` VALUES (11, 11, 35, 26, 4, 1, 199.00);
INSERT INTO `order_item` VALUES (12, 12, 36, 28, 4, 1, 89.00);
INSERT INTO `order_item` VALUES (13, 12, 37, 29, 4, 1, 149.00);
INSERT INTO `order_item` VALUES (14, 13, 38, 30, 4, 1, 299.00);
INSERT INTO `order_item` VALUES (15, 14, 39, 31, 4, 2, 79.00);
INSERT INTO `order_item` VALUES (16, 15, 40, 33, 4, 1, 25.00);
INSERT INTO `order_item` VALUES (17, 15, 42, 35, 4, 1, 35.00);
INSERT INTO `order_item` VALUES (18, 16, 41, 34, 4, 1, 35.00);
INSERT INTO `order_item` VALUES (19, 681315182, 35, 35, 4, 2, 199.00);
INSERT INTO `order_item` VALUES (20, 682094711, 25, 25, 2, 1, 1999.00);
INSERT INTO `order_item` VALUES (21, 689787121, 35, 35, 4, 1, 199.00);
INSERT INTO `order_item` VALUES (22, 1781692120488607, 36, 27, 4, 2, 89.00);
INSERT INTO `order_item` VALUES (23, 1781694777837887, 38, 30, 4, 3, 299.00);
INSERT INTO `order_item` VALUES (24, 1781699253446194, 36, 27, 4, 2, 89.00);
INSERT INTO `order_item` VALUES (25, 1781702033464704, 1, 1, 1, 1, 6299.00);
INSERT INTO `order_item` VALUES (26, 1781703002071410, 35, 25, 4, 1, 199.00);
INSERT INTO `order_item` VALUES (27, 1781703393576237, 38, 30, 4, 2, 299.00);
INSERT INTO `order_item` VALUES (28, 1781704453093283, 36, 27, 4, 1, 89.00);
INSERT INTO `order_item` VALUES (29, 1781705096378138, 35, 25, 4, 1, 199.00);
INSERT INTO `order_item` VALUES (30, 1781705692745192, 41, 34, 4, 1, 35.00);
INSERT INTO `order_item` VALUES (31, 1781707699162231, 42, 35, 4, 1, 59.00);
INSERT INTO `order_item` VALUES (32, 1781710682086876, 36, 27, 4, 1, 89.00);
INSERT INTO `order_item` VALUES (33, 1781807398158622, 25, 18, 2, 1, 1999.00);
INSERT INTO `order_item` VALUES (34, 1781809988280696, 42, 35, 4, 1, 59.00);
INSERT INTO `order_item` VALUES (35, 1781809988280696, 38, 30, 4, 1, 299.00);
INSERT INTO `order_item` VALUES (36, 1781843358719375, 38, 30, 4, 1, 299.00);
INSERT INTO `order_item` VALUES (37, 1781953401354776, 21, 21, 2, 1, 3.50);
INSERT INTO `order_item` VALUES (38, 1781953623001191, 9, 45, 2, 1, 199.00);
INSERT INTO `order_item` VALUES (39, 1781954784780346, 9, 45, 2, 1, 199.00);
INSERT INTO `order_item` VALUES (40, 1781959638288248, 20, 58, 2, 1, 5.90);
INSERT INTO `order_item` VALUES (41, 1781959731977207, 20, 58, 2, 1, 5.90);
INSERT INTO `order_item` VALUES (42, 1781959980307245, 18, 54, 2, 1, 19.90);
INSERT INTO `order_item` VALUES (43, 1781960396631301, 20, 58, 2, 1, 5.90);
INSERT INTO `order_item` VALUES (44, 1781960748742517, 36, 27, 4, 1, 89.00);
INSERT INTO `order_item` VALUES (45, 1781961436172138, 36, 27, 4, 1, 89.00);
INSERT INTO `order_item` VALUES (46, 1781961933307323, 15, 50, 3, 1, 899.00);
INSERT INTO `order_item` VALUES (47, 1781962027104967, 19, 56, 2, 1, 39.90);
INSERT INTO `order_item` VALUES (48, 1781962330495605, 39, 31, 4, 1, 79.00);
INSERT INTO `order_item` VALUES (49, 1781963346148917, 9, 45, 2, 1, 199.00);
INSERT INTO `order_item` VALUES (50, 1781963445095651, 9, 45, 2, 1, 199.00);
INSERT INTO `order_item` VALUES (51, 1782115012350557, 24, 17, 1, 3, 2990.00);
INSERT INTO `order_item` VALUES (52, 1782115036700971, 26, 62, 1, 3, 399.00);
INSERT INTO `order_item` VALUES (53, 1782115044209811, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (54, 1782115051107836, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (55, 1782115109670249, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (56, 1782122196589217, 26, 62, 1, 4, 399.00);
INSERT INTO `order_item` VALUES (57, 1782128782279589, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (58, 1782128903382833, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (59, 1782130209526651, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (60, 1782130230416930, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (61, 1782130273471723, 26, 62, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (62, 1782132035009573, 26, 63, 1, 1, 399.00);
INSERT INTO `order_item` VALUES (63, 1782141603227404, 42, 93, 4, 1, 59.00);
INSERT INTO `order_item` VALUES (64, 1782141613179855, 42, 93, 4, 1, 59.00);
INSERT INTO `order_item` VALUES (65, 1782141716900986, 42, 93, 4, 2, 59.00);
INSERT INTO `order_item` VALUES (66, 1782142895673456, 42, 93, 4, 5, 59.00);
INSERT INTO `order_item` VALUES (67, 1782144530313583, 3, 38, 2, 3, 6499.00);
INSERT INTO `order_item` VALUES (68, 1782144544078226, 42, 93, 4, 4, 79.00);
INSERT INTO `order_item` VALUES (69, 1782144573137672, 42, 35, 4, 2, 59.00);
INSERT INTO `order_item` VALUES (70, 1782144626395758, 42, 93, 4, 1, 79.00);
INSERT INTO `order_item` VALUES (71, 1782144634286833, 42, 35, 4, 3, 59.00);
INSERT INTO `order_item` VALUES (72, 1782144642249551, 42, 93, 4, 4, 79.00);
INSERT INTO `order_item` VALUES (73, 1782144657590788, 9, 46, 2, 5, 199.00);
INSERT INTO `order_item` VALUES (74, 1782147308134823, 42, 92, 4, 2, 59.00);
INSERT INTO `order_item` VALUES (75, 1782223044998905, 9, 45, 2, 1, 199.00);
INSERT INTO `order_item` VALUES (76, 1782223340907509, 42, 35, 4, 1, 59.00);
INSERT INTO `order_item` VALUES (77, 1782223412658349, 42, 35, 4, 5, 59.00);
INSERT INTO `order_item` VALUES (82, 1783601656671419, 1, 1, 1, 1, 99.00);
INSERT INTO `order_item` VALUES (83, 1783601656671420, 2, 3, 1, 1, 199.00);
INSERT INTO `order_item` VALUES (84, 1783601656671421, 4, 39, 1, 1, 299.00);
INSERT INTO `order_item` VALUES (85, 1783601656671422, 2, 3, 1, 1, 199.00);
INSERT INTO `order_item` VALUES (86, 1783601656671423, 5, 5, 2, 1, 79.00);
INSERT INTO `order_item` VALUES (87, 1783601656671424, 1, 1, 1, 1, 99.00);

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一订单编号',
  `user_id` bigint NOT NULL,
  `total_price` decimal(10, 2) NOT NULL COMMENT '商品总价',
  `coupon_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  `actual_price` decimal(10, 2) NOT NULL COMMENT '实付金额',
  `pay_type` tinyint NULL DEFAULT 1 COMMENT '1支付宝',
  `pay_status` tinyint NULL DEFAULT 0 COMMENT '0未付1已付',
  `order_status` tinyint NULL DEFAULT 0 COMMENT '0待付款1待发货2待收货3已完成4已取消',
  `address_id` bigint NOT NULL,
  `logistics` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物流单号',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '?????????????',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_order_no`(`order_no`) USING BTREE,
  INDEX `idx_uid`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1783601656671633 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'ORD20250601001', 1, 6299.00, 300.00, 5999.00, 1, 1, 3, 1, 'SF12009871', NULL, '2026-06-07 13:54:04');
INSERT INTO `orders` VALUES (2, 'ORD20250601002', 2, 269.00, 20.00, 249.00, 1, 1, 2, 2, 'YT66251782', NULL, '2026-06-07 13:54:04');
INSERT INTO `orders` VALUES (3, 'ORD20250601003', 3, 69.90, 0.00, 69.90, 1, 1, 3, 3, 'ZT88721671', NULL, '2026-06-07 13:54:04');
INSERT INTO `orders` VALUES (4, 'ORD20260611001', 1, 99.00, 0.00, 99.00, 1, 0, 1, 1, '', NULL, '2026-06-11 00:17:55');
INSERT INTO `orders` VALUES (5, 'ORD20260611002', 2, 199.00, 0.00, 199.00, 1, 1, 2, 2, '', NULL, '2026-06-11 00:17:55');
INSERT INTO `orders` VALUES (6, 'ORD20260611003', 3, 299.00, 0.00, 299.00, 1, 1, 3, 3, 'SF1234567890', NULL, '2026-06-11 00:17:55');
INSERT INTO `orders` VALUES (7, 'ORD20260614007', 3, 269.00, 0.00, 269.00, 1, 0, 4, 3, '', NULL, '2026-06-14 15:30:00');
INSERT INTO `orders` VALUES (8, 'ORD20260614008', 3, 189.00, 0.00, 189.00, 1, 1, 1, 3, '', NULL, '2026-06-14 14:00:00');
INSERT INTO `orders` VALUES (9, 'ORD20260614009', 3, 299.00, 0.00, 299.00, 1, 1, 3, 3, 'YT99887766', NULL, '2026-06-14 13:00:00');
INSERT INTO `orders` VALUES (10, 'ORD20260614010', 3, 79.90, 0.00, 79.90, 1, 0, 4, 3, '', NULL, '2026-06-14 12:00:00');
INSERT INTO `orders` VALUES (11, 'ORD20260614011', 1, 199.00, 10.00, 189.00, 1, 1, 3, 1, 'SF1111111111', '王五数码店测试订单-已完成', '2026-06-12 10:00:00');
INSERT INTO `orders` VALUES (12, 'ORD20260614012', 2, 238.00, 15.00, 223.00, 1, 1, 3, 2, 'SF2222222222', '王五数码店测试订单-已完成', '2026-06-12 14:00:00');
INSERT INTO `orders` VALUES (13, 'ORD20260614013', 1, 299.00, 0.00, 299.00, 1, 1, 2, 1, '顺丰速运:2', NULL, '2026-06-14 09:00:00');
INSERT INTO `orders` VALUES (14, 'ORD20260614014', 2, 158.00, 0.00, 158.00, 1, 1, 2, 2, 'YT3333333333', NULL, '2026-06-13 10:00:00');
INSERT INTO `orders` VALUES (15, 'ORD20260614015', 1, 60.00, 0.00, 60.00, 1, 0, 0, 1, '', NULL, '2026-06-14 16:00:00');
INSERT INTO `orders` VALUES (16, 'ORD20260614016', 2, 35.00, 0.00, 35.00, 1, 0, 4, 2, '', NULL, '2026-06-13 18:00:00');
INSERT INTO `orders` VALUES (681315182, 'OD17816813151775681315182', 5, 398.00, 0.00, 398.00, 0, 0, 4, 5, NULL, NULL, '2026-06-17 15:28:35');
INSERT INTO `orders` VALUES (682094711, 'OD17816820947065682094711', 5, 1999.00, 0.00, 1999.00, 0, 0, 4, 5, NULL, NULL, '2026-06-17 15:41:34');
INSERT INTO `orders` VALUES (689787121, 'OD17816897871165689787121', 5, 199.00, 0.00, 199.00, 0, 0, 4, 5, NULL, NULL, '2026-06-17 17:49:47');
INSERT INTO `orders` VALUES (1781692120488607, 'OD1781692120488607', 5, 178.00, 0.00, 178.00, 0, 0, 4, 5, NULL, '', '2026-06-17 18:28:40');
INSERT INTO `orders` VALUES (1781694777837887, 'OD1781694777837887', 5, 897.00, 0.00, 897.00, 0, 0, 4, 5, NULL, '', '2026-06-17 19:12:57');
INSERT INTO `orders` VALUES (1781699253446194, 'OD1781699253446194', 5, 178.00, 0.00, 178.00, 0, 0, 4, 5, NULL, '', '2026-06-17 20:27:33');
INSERT INTO `orders` VALUES (1781702033464704, 'OD1781702033464704', 3, 6299.00, 0.00, 6299.00, 0, 0, 4, 3, NULL, '', '2026-06-17 21:13:53');
INSERT INTO `orders` VALUES (1781703002071410, 'OD1781703002071410', 3, 199.00, 0.00, 199.00, 0, 1, 2, 3, '顺丰速运:1', '', '2026-06-17 21:30:02');
INSERT INTO `orders` VALUES (1781703393576237, 'OD1781703393576237', 3, 598.00, 0.00, 598.00, 0, 0, 4, 3, NULL, '', '2026-06-17 21:36:33');
INSERT INTO `orders` VALUES (1781704453093283, 'OD1781704453093283', 3, 89.00, 0.00, 89.00, 0, 0, 4, 3, NULL, '', '2026-06-17 21:54:13');
INSERT INTO `orders` VALUES (1781705096378138, 'OD1781705096378138', 3, 199.00, 0.00, 199.00, 0, 0, 4, 3, NULL, '', '2026-06-17 22:04:56');
INSERT INTO `orders` VALUES (1781705692745192, 'OD1781705692745192', 3, 35.00, 0.00, 35.00, 0, 1, 3, 3, '顺丰速运:	OD1781809988280696', '', '2026-06-17 22:14:52');
INSERT INTO `orders` VALUES (1781707699162231, 'OD1781707699162231', 3, 59.00, 0.00, 59.00, 0, 1, 3, 3, '顺丰速运:	OD1781809988280696', '', '2026-06-17 22:48:19');
INSERT INTO `orders` VALUES (1781710682086876, 'OD1781710682086876', 3, 89.00, 20.00, 69.00, 0, 1, 3, 3, '顺丰速运:	OD1781809988280696', '', '2026-06-17 23:38:02');
INSERT INTO `orders` VALUES (1781807398158622, 'OD1781807398158622', 3, 1999.00, 0.00, 1999.00, 0, 0, 4, 3, NULL, '', '2026-06-19 02:29:58');
INSERT INTO `orders` VALUES (1781809988280696, 'OD1781809988280696', 3, 358.00, 0.00, 358.00, 0, 0, 4, 3, NULL, '', '2026-06-19 03:13:08');
INSERT INTO `orders` VALUES (1781843358719375, 'OD1781843358719375', 3, 299.00, 0.00, 299.00, 0, 0, 4, 3, NULL, '', '2026-06-19 12:29:18');
INSERT INTO `orders` VALUES (1781953401354776, 'OD1781953401354776', 3, 3.50, 0.00, 3.50, 0, 0, 4, 3, NULL, '', '2026-06-20 19:03:21');
INSERT INTO `orders` VALUES (1781953623001191, 'OD1781953623001191', 3, 199.00, 0.00, 199.00, 0, 0, 4, 3, NULL, '', '2026-06-20 19:07:03');
INSERT INTO `orders` VALUES (1781954784780346, 'OD1781954784780346', 3, 199.00, 0.00, 199.00, 0, 0, 4, 3, NULL, '', '2026-06-20 19:26:24');
INSERT INTO `orders` VALUES (1781959638288248, 'OD1781959638288248', 3, 5.90, 0.00, 5.90, 1, 1, 1, 3, NULL, '', '2026-06-20 20:47:18');
INSERT INTO `orders` VALUES (1781959731977207, 'OD1781959731977207', 3, 5.90, 0.00, 5.90, 0, 0, 4, 3, NULL, '', '2026-06-20 20:48:51');
INSERT INTO `orders` VALUES (1781959980307245, 'OD1781959980307245', 5, 19.90, 0.00, 19.90, 0, 0, 4, 5, NULL, '', '2026-06-20 20:53:00');
INSERT INTO `orders` VALUES (1781960396631301, 'OD1781960396631301', 3, 5.90, 0.00, 5.90, 0, 0, 4, 3, NULL, '', '2026-06-20 20:59:56');
INSERT INTO `orders` VALUES (1781960748742517, 'OD1781960748742517', 5, 89.00, 0.00, 89.00, 0, 0, 4, 5, NULL, '', '2026-06-20 21:05:48');
INSERT INTO `orders` VALUES (1781961436172138, 'OD1781961436172138', 5, 89.00, 0.00, 89.00, 0, 0, 4, 5, NULL, '', '2026-06-20 21:17:16');
INSERT INTO `orders` VALUES (1781961933307323, 'OD1781961933307323', 3, 899.00, 0.00, 899.00, 1, 1, 1, 3, NULL, '', '2026-06-20 21:25:33');
INSERT INTO `orders` VALUES (1781962027104967, 'OD1781962027104967', 3, 39.90, 0.00, 39.90, 1, 1, 1, 3, NULL, '', '2026-06-20 21:27:07');
INSERT INTO `orders` VALUES (1781962330495605, 'OD1781962330495605', 3, 79.00, 0.00, 79.00, 1, 1, 1, 3, NULL, '', '2026-06-20 21:32:10');
INSERT INTO `orders` VALUES (1781963346148917, 'OD1781963346148917', 5, 199.00, 0.00, 199.00, 0, 0, 4, 5, NULL, '', '2026-06-20 21:49:06');
INSERT INTO `orders` VALUES (1781963445095651, 'OD1781963445095651', 3, 199.00, 0.00, 199.00, 1, 1, 1, 3, NULL, '', '2026-06-20 21:50:45');
INSERT INTO `orders` VALUES (1782115012350557, 'OD1782115012350557', 5, 8970.00, 0.00, 8970.00, 0, 0, 4, 5, NULL, '', '2026-06-22 15:56:52');
INSERT INTO `orders` VALUES (1782115036700971, 'OD1782115036700971', 5, 1197.00, 0.00, 1197.00, 0, 0, 4, 5, NULL, '', '2026-06-22 15:57:16');
INSERT INTO `orders` VALUES (1782115044209811, 'OD1782115044209811', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 15:57:24');
INSERT INTO `orders` VALUES (1782115051107836, 'OD1782115051107836', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 15:57:31');
INSERT INTO `orders` VALUES (1782115109670249, 'OD1782115109670249', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 15:58:29');
INSERT INTO `orders` VALUES (1782122196589217, 'OD1782122196589217', 5, 1596.00, 0.00, 1596.00, 0, 0, 4, 5, NULL, '', '2026-06-22 17:56:36');
INSERT INTO `orders` VALUES (1782128782279589, 'OD1782128782279589', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 19:46:22');
INSERT INTO `orders` VALUES (1782128903382833, 'OD1782128903382833', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 19:48:23');
INSERT INTO `orders` VALUES (1782130209526651, 'OD1782130209526651', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 20:10:09');
INSERT INTO `orders` VALUES (1782130230416930, 'OD1782130230416930', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 20:10:30');
INSERT INTO `orders` VALUES (1782130273471723, 'OD1782130273471723', 5, 399.00, 0.00, 399.00, 0, 0, 4, 5, NULL, '', '2026-06-22 20:11:13');
INSERT INTO `orders` VALUES (1782132035009573, 'OD1782132035009573', 5, 599.00, 0.00, 599.00, 0, 0, 4, 5, NULL, '', '2026-06-22 20:40:35');
INSERT INTO `orders` VALUES (1782141603227404, 'OD1782141603227404', 3, 79.00, 0.00, 79.00, 0, 0, 4, 3, NULL, '', '2026-06-22 23:20:03');
INSERT INTO `orders` VALUES (1782141613179855, 'OD1782141613179855', 3, 79.00, 0.00, 79.00, 0, 0, 4, 3, NULL, '', '2026-06-22 23:20:13');
INSERT INTO `orders` VALUES (1782141716900986, 'OD1782141716900986', 5, 158.00, 0.00, 158.00, 1, 1, 3, 5, '顺丰速运:1', '', '2026-06-22 23:21:56');
INSERT INTO `orders` VALUES (1782142895673456, 'OD1782142895673456', 5, 395.00, 0.00, 395.00, 0, 0, 4, 5, NULL, '', '2026-06-22 23:41:35');
INSERT INTO `orders` VALUES (1782144530313583, 'OD1782144530313583', 5, 19497.00, 0.00, 19497.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:08:50');
INSERT INTO `orders` VALUES (1782144544078226, 'OD1782144544078226', 5, 316.00, 0.00, 316.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:09:04');
INSERT INTO `orders` VALUES (1782144573137672, 'OD1782144573137672', 5, 118.00, 0.00, 118.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:09:33');
INSERT INTO `orders` VALUES (1782144626395758, 'OD1782144626395758', 5, 79.00, 0.00, 79.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:10:26');
INSERT INTO `orders` VALUES (1782144634286833, 'OD1782144634286833', 5, 177.00, 0.00, 177.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:10:34');
INSERT INTO `orders` VALUES (1782144642249551, 'OD1782144642249551', 5, 316.00, 0.00, 316.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:10:42');
INSERT INTO `orders` VALUES (1782144657590788, 'OD1782144657590788', 5, 995.00, 0.00, 995.00, 0, 0, 4, 5, NULL, '', '2026-06-23 00:10:57');
INSERT INTO `orders` VALUES (1782147308134823, 'OD1782147308134823', 5, 118.00, 0.00, 118.00, 1, 1, 1, 5, NULL, '', '2026-06-23 00:55:08');
INSERT INTO `orders` VALUES (1782223044998905, 'OD1782223044998905', 3, 199.00, 0.00, 199.00, 1, 1, 1, 3, NULL, '', '2026-06-23 21:57:25');
INSERT INTO `orders` VALUES (1782223340907509, 'OD1782223340907509', 3, 59.00, 0.00, 59.00, 1, 1, 1, 3, NULL, '', '2026-06-23 22:02:20');
INSERT INTO `orders` VALUES (1782223412658349, 'OD1782223412658349', 3, 295.00, 0.00, 295.00, 1, 1, 1, 3, NULL, '', '2026-06-23 22:03:32');
INSERT INTO `orders` VALUES (1782229583765212, 'OD1782229583765212', 3, 199.00, 20.00, 179.00, 1, 1, 1, 3, NULL, '', '2026-06-23 23:46:23');
INSERT INTO `orders` VALUES (1782292520205745, 'OD1782292520205745', 3, 5.90, 0.00, 5.90, 1, 1, 1, 3, NULL, '', '2026-06-24 17:15:20');
INSERT INTO `orders` VALUES (1783601571842970, 'OD1783601571842970', 12, 35.00, 0.00, 35.00, 0, 0, 4, 7, NULL, '', '2026-07-09 20:52:51');
INSERT INTO `orders` VALUES (1783601656671407, 'OD1783601656671407', 12, 4999.00, 0.00, 4999.00, 1, 1, 1, 7, NULL, '', '2026-07-09 20:54:16');
INSERT INTO `orders` VALUES (1783601656671420, 'FS17840375591333', 3, 199.00, 0.00, 199.00, 1, 0, 0, 3, NULL, NULL, '2026-08-11 17:54:45');
INSERT INTO `orders` VALUES (1783601656671421, 'FS17862893541463', 3, 299.00, 0.00, 299.00, 1, 0, 0, 3, NULL, NULL, '2026-08-11 17:54:45');
INSERT INTO `orders` VALUES (1783601656671422, 'FS17862896570183', 3, 199.00, 0.00, 199.00, 1, 0, 0, 3, NULL, NULL, '2026-08-11 17:54:46');
INSERT INTO `orders` VALUES (1783601656671423, 'FS17862903458163', 3, 79.00, 0.00, 79.00, 1, 0, 0, 3, NULL, NULL, '2026-08-11 17:54:46');
INSERT INTO `orders` VALUES (1783601656671424, 'FS17862907306863', 3, 99.00, 0.00, 99.00, 1, 1, 1, 3, NULL, NULL, '2026-08-11 17:54:46');

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '接口/路由地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permission
-- ----------------------------
INSERT INTO `permission` VALUES (1, '用户管理', '/admin/user');
INSERT INTO `permission` VALUES (2, '商品管理', '/admin/product');
INSERT INTO `permission` VALUES (3, '订单管理', '/admin/order');
INSERT INTO `permission` VALUES (4, '分类管理', '/admin/category');
INSERT INTO `permission` VALUES (5, '优惠券管理', '/admin/coupon');
INSERT INTO `permission` VALUES (6, '售后管理', '/admin/aftersale');

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint NOT NULL COMMENT '分类id',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（多商户）',
  `price` decimal(10, 2) NOT NULL COMMENT '售价',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品主图',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0下架1上架',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `view_point` int NULL DEFAULT 0 COMMENT '热度分（基于浏览量累积）',
  `sales` int NULL DEFAULT 0 COMMENT '真实销量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cid`(`category_id`) USING BTREE,
  INDEX `idx_shop_id`(`shop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, '华为Mate60 Pro', 6, 1, 6299.00, '/api/upload/产品图/华为Mate60Pro/华为Mate60Pro(2).jpg', 1, '华为旗舰手机，搭载麒麟芯片', '2026-06-07 13:54:04', 3727, 2);
INSERT INTO `product` VALUES (2, 'iPhone 15 Pro Max', 6, 1, 9999.00, '/api/upload/产品图/iPhone15ProMax/iPhone15ProMax(2).jpg', 1, '苹果旗舰手机，钛金属机身', '2026-06-07 13:54:04', 224, 0);
INSERT INTO `product` VALUES (3, '小米14 Ultra', 6, 2, 5999.00, '/api/upload/产品图/小米14Ultra/OIPCBXCqfKGoIGgwWWukpjTUszVlKCPf.jpg', 1, '小米影像旗舰，徕卡联合调校', '2026-06-07 13:54:04', 4842, 3);
INSERT INTO `product` VALUES (4, 'iPad Pro 12.9寸', 7, 1, 8999.00, '/api/upload/产品图/iPadPro12.9寸/OIPCCzPlqkvlUeWxAUppnvFlxUgYmZHC.jpg', 1, 'M2芯片，全面屏设计', '2026-06-07 13:54:04', 3436, 0);
INSERT INTO `product` VALUES (5, '联想小新Pro16', 8, 2, 4999.00, '/api/upload/产品图/联想小新Pro16/BQBMskuQSdsVGEOmzKylKetVjeiIHwEl.jpg', 1, '高性能轻薄本，学生党首选', '2026-06-07 13:54:04', 2557, 1);
INSERT INTO `product` VALUES (6, 'Apple Watch Series 9', 9, 1, 2999.00, '/api/upload/产品图/AppleWatchSeries9/LiNfmvjopqcvzuSgAAAAOedeUzCYUGTe.jpg', 1, '健康监测，全天候显示', '2026-06-07 13:54:04', 2376, 0);
INSERT INTO `product` VALUES (7, '索尼WH-1000XM5耳机', 10, 1, 2499.00, '/api/upload/产品图/索尼WH-1000XM5耳机/bTVkAAAodtEgKSJsQJoKzPmjWcBiMXcs.jpg', 1, '顶级降噪耳机，音质纯净', '2026-06-07 13:54:04', 4111, 0);
INSERT INTO `product` VALUES (8, '耐克运动卫衣', 11, 2, 269.00, '/api/upload/产品图/耐克运动卫衣/jOrdkOkViLZlIRtwAIijznGiPQiMWvZQ.jpg', 1, '纯棉透气运动上衣，宽松版型', '2026-06-07 13:54:04', 3329, 1);
INSERT INTO `product` VALUES (9, '优衣库基础款牛仔裤', 11, 2, 199.00, '/api/upload/产品图/优衣库基础款牛仔裤/OIPCBUqBzIxDnIDzIqURLODrMEXaPUmu.jpg', 1, '经典直筒，百搭不挑身材', '2026-06-07 13:54:04', 4209, 9);
INSERT INTO `product` VALUES (10, 'ZARA法式连衣裙', 12, 3, 399.00, '/api/upload/产品图/ZARA法式连衣裙/dAudJOGkhufTzeEHEreFDAODyuAAAAAA.jpg', 1, '收腰显瘦，夏季必备', '2026-06-07 13:54:04', 962, 0);
INSERT INTO `product` VALUES (11, '太平鸟雪纺衬衫', 12, 3, 259.00, '/api/upload/产品图/太平鸟雪纺衬衫/OIPCeIzrvPnJwwTzodwNIrWdhMFUoZRq.jpg', 1, '轻薄透气，通勤百搭', '2026-06-07 13:54:04', 2080, 0);
INSERT INTO `product` VALUES (12, 'AJ 1 高帮板鞋', 13, 1, 1299.00, '/api/upload/产品图/AJ1高帮板鞋/dOczMEzEcrKiLFoASqSzTmIHTpmXAAAY.jpg', 1, '经典款，街头潮流必备', '2026-06-07 13:54:04', 2416, 0);
INSERT INTO `product` VALUES (13, '安踏跑步鞋', 13, 2, 399.00, '/api/upload/产品图/安踏跑步鞋/FNAcAAAAAZfQzIFhuHMOjgEeZWRIYDQZ.jpg', 1, '缓震科技，轻盈透气', '2026-06-07 13:54:04', 742, 0);
INSERT INTO `product` VALUES (14, 'Coach托特包', 14, 3, 2999.00, '/api/upload/产品图/Coach托特包/bwAjHAQQjUoBFUQAAAASCmyBmDtNooYb.jpg', 1, '大容量通勤包，耐脏耐磨', '2026-06-07 13:54:04', 1363, 0);
INSERT INTO `product` VALUES (15, '施华洛世奇项链', 15, 3, 899.00, '/api/upload/产品图/施华洛世奇项链/OIPCBSLcVpNkbXxeKyLmPcaTemmvNAAC.jpg', 1, '水晶镶嵌，百搭饰品', '2026-06-07 13:54:04', 4488, 1);
INSERT INTO `product` VALUES (16, '三只松鼠坚果礼盒', 16, 2, 79.90, '/api/upload/产品图/三只松鼠坚果礼盒/BAAAACOnOEMQRgBBicYFrpsLgvBSqStB.jpg', 1, '年货坚果大礼包，多种口味', '2026-06-07 13:54:04', 3253, 3);
INSERT INTO `product` VALUES (17, '百草味夏威夷果', 16, 2, 29.90, '/api/upload/产品图/百草味夏威夷果/hLriuljASAAvgMAAAAwoVDtOMINWgaaf.jpg', 1, '大颗粒，奶香味浓郁', '2026-06-07 13:54:04', 2700, 0);
INSERT INTO `product` VALUES (18, '奥利奥夹心饼干', 17, 2, 19.90, '/api/upload/产品图/奥利奥夹心饼干/nsoeizXudMncSbIJSfDSVUmSaTCSVkTF.jpg', 1, '经典巧克力味，酥脆可口', '2026-06-07 13:54:04', 3640, 1);
INSERT INTO `product` VALUES (19, '良品铺子猪肉脯', 18, 2, 39.90, '/api/upload/产品图/良品铺子猪肉脯/FgCJAAAIGYKsXMNBHGoDNUhbkwlICvfB.jpg', 1, '蜜汁风味，嚼劲十足', '2026-06-07 13:54:04', 5002, 1);
INSERT INTO `product` VALUES (20, '元气森林气泡水', 19, 2, 5.90, '/api/upload/产品图/元气森林气泡水/GgGeUamasSbLvuYAiRFKGUvjeAAAAAAA.jpg', 1, '0糖0脂，清爽解渴', '2026-06-07 13:54:04', 3992, 3);
INSERT INTO `product` VALUES (21, '康师傅红烧牛肉面', 20, 2, 3.50, '/api/upload/产品图/康师傅红烧牛肉面/aHWtgByjaUfvETxucUEnizCIAuPEDuDG.jpg', 1, '经典口味，方便速食', '2026-06-07 13:54:04', 4852, 1);
INSERT INTO `product` VALUES (22, '美的电饭煲', 21, 2, 189.00, '/api/upload/产品图/美的电饭煲/LjzAAAAAAAAAArYWcxJHLSKtjkTTJuOS.jpg', 1, '4L智能家用，一键煮饭', '2026-06-07 13:54:04', 2187, 2);
INSERT INTO `product` VALUES (23, '苏泊尔空气炸锅', 21, 2, 299.00, '/api/upload/产品图/苏泊尔空气炸锅/DwotzsNIxRJmdrljzAIHnACGbgsMNffv.jpg', 1, '无油烹饪，健康低脂', '2026-06-07 13:54:04', 1281, 1);
INSERT INTO `product` VALUES (24, '戴森吹风机', 22, 1, 2990.00, '/api/upload/产品图/戴森吹风机/HodqQWAnURuXKDzgpGCmTqCBzWuSDddx.jpg', 1, '负离子护发，快速干发', '2026-06-07 13:54:04', 4739, 3);
INSERT INTO `product` VALUES (25, '小米扫地机器人', 22, 2, 1999.00, '/api/upload/产品图/小米扫地机器人/bLCttkbhWwUACZmuetMptIAAVdkoyQzp.jpg', 1, '自动清扫，解放双手', '2026-06-07 13:54:04', 4755, 2);
INSERT INTO `product` VALUES (26, '飞利浦电动牙刷', 23, 1, 399.00, '/api/upload/产品图/飞利浦电动牙刷/AXPBHxQsGWjAXQChBVbiDhVAAAAAJDMP.jpg', 1, '声波震动，深层清洁', '2026-06-07 13:54:04', 4460, 16);
INSERT INTO `product` VALUES (27, '海尔冰箱双开门', 24, 2, 3999.00, '/api/upload/产品图/海尔冰箱双开门/bgXCBaxsVZfFrOLsgNGgAAAASNwuzRRT.jpg', 1, '大容量，风冷无霜', '2026-06-07 13:54:04', 2935, 0);
INSERT INTO `product` VALUES (28, '补水面膜', 26, 3, 69.90, '/api/upload/产品图/补水面膜/BkcHccOuVTetiKABTvQbDgPouAUKNdAQ.jpg', 1, '玻尿酸保湿，补水修护', '2026-06-07 13:54:04', 1194, 3);
INSERT INTO `product` VALUES (29, 'SK-II神仙水', 26, 3, 1590.00, '/api/upload/产品图/SK-II神仙水/DnoyjJarLHmYeubIwqSjiiigDCSqVHyQ.jpg', 1, '调节水油平衡，改善肤质', '2026-06-07 13:54:04', 2066, 0);
INSERT INTO `product` VALUES (30, 'YSL小金条口红', 27, 3, 380.00, '/api/upload/产品图/YSL小金条口红/BTvWAHRIANNpqmSMSoPGFKRJYFgMeNdW.jpg', 1, '哑光质地，显白不挑皮', '2026-06-07 13:54:04', 1647, 0);
INSERT INTO `product` VALUES (31, '迪奥999口红', 27, 3, 350.00, '/api/upload/产品图/迪奥999口红/DEnBNgMNQWnPBawabxlApIdKqctNjXsB.jpg', 1, '正红色，气场全开', '2026-06-07 13:54:04', 1940, 0);
INSERT INTO `product` VALUES (32, '香奈儿邂逅香水', 28, 3, 1090.00, '/api/upload/产品图/香奈儿邂逅香水/OIPCijOsvBeBkNCdPxdynvFAVZIXxEwh.jpg', 1, '清新花果香，持久留香', '2026-06-07 13:54:04', 4660, 0);
INSERT INTO `product` VALUES (33, '舒肤佳沐浴露', 29, 2, 29.90, '/api/upload/产品图/舒肤佳沐浴露/DokzuAsvgtvryTfHQySYgAAAMcjsIkaV.jpg', 1, '温和清洁，留香持久', '2026-06-07 13:54:04', 2380, 0);
INSERT INTO `product` VALUES (34, '植村秀化妆刷套装', 30, 3, 199.00, '/api/upload/产品图/植村秀化妆刷套装/JHhMSQwAAAAAAAOPAzAtRrkDWWmklRfY.jpg', 1, '柔软刷毛，上妆服帖', '2026-06-07 13:54:04', 2821, 0);
INSERT INTO `product` VALUES (35, '无线蓝牙耳机 AirPro', 1, 4, 199.00, '/api/upload/产品图/无线蓝牙耳机AirPro/FJAANyAAAAActQsccIcOoxWADGGubmSB.jpg', 1, '高品质无线蓝牙耳机，续航24小时，佩戴舒适', '2026-06-10 10:00:00', 1864, 6);
INSERT INTO `product` VALUES (36, '20000mAh快充充电宝', 1, 4, 89.00, '/api/upload/产品图/20000mAh快充充电宝/fEaSFdgAAAnKkYcQpvtQqpUvqKedkgzU.jpg', 1, '22.5W超级快充，轻薄便携，飞机可带', '2026-06-10 10:00:00', 757, 9);
INSERT INTO `product` VALUES (37, 'Type-C 7合1扩展坞', 1, 4, 149.00, '/api/upload/产品图/Type-C7合1扩展坞/AHqVnhXPgCoJSEubwAAAASXDIAqQVonZ.jpg', 1, 'HDMI/USB3.0/PD快充，兼容苹果笔记本', '2026-06-10 10:00:00', 3090, 1);
INSERT INTO `product` VALUES (38, 'RGB机械键盘 青轴', 1, 4, 299.00, '/api/upload/产品图/RGB机械键盘青轴/dAXZzcDFinSBHpePTqBjYcwCmHIkaCpV.jpg', 1, '87键紧凑布局，RGB背光，青轴段落手感', '2026-06-10 10:00:00', 3084, 8);
INSERT INTO `product` VALUES (39, '静音无线鼠标', 1, 4, 79.00, '/api/upload/产品图/静音无线鼠标/IFFbFxbgZyopJKRHABqRAEcKAAAAAwAA.jpg', 1, '2.4G+蓝牙双模，静音按键，人体工学', '2026-06-10 10:00:00', 1048, 3);
INSERT INTO `product` VALUES (40, '手机直播支架 落地款', 1, 4, 25.00, '/api/upload/产品图/手机直播支架落地款/aslDCUmQoHniWBkMXuimCHzREaXqBkrI.jpg', 1, '可升降落地支架，手机/平板通用', '2026-06-10 10:00:00', 890, 1);
INSERT INTO `product` VALUES (41, '三合一快充数据线', 1, 4, 35.00, '/api/upload/产品图/三合一快充数据线/nFwAAGJSWlzHoTlnwMygFLBBUaOedyrm.jpg', 1, '苹果/Type-C/MicroUSB三合一，1.2米', '2026-06-10 10:00:00', 1209, 2);
INSERT INTO `product` VALUES (42, '桌面USB小风扇', 1, 4, 59.00, '/api/upload/产品图/桌面USB小风扇/hmEozRCfylgiWYHiAAAAAAiCfBSsvynP.jpg', 1, '3档风力，静音运行，USB供电', '2026-06-10 10:00:00', 3271, 28);

-- ----------------------------
-- Table structure for product_image
-- ----------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pid`(`product_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 208 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_image
-- ----------------------------
INSERT INTO `product_image` VALUES (1, 1, '/api/upload/产品图/华为Mate60Pro/华为Mate60Pro(2).jpg', 0);
INSERT INTO `product_image` VALUES (2, 1, '/api/upload/产品图/华为Mate60Pro/华为Mate60Pro.jpg', 1);
INSERT INTO `product_image` VALUES (3, 1, '/api/upload/产品图/华为Mate60Pro/华为Mate60Pro_1.jpg', 2);
INSERT INTO `product_image` VALUES (4, 2, '/api/upload/产品图/iPhone15ProMax/iPhone15ProMax(2).jpg', 0);
INSERT INTO `product_image` VALUES (5, 2, '/api/upload/产品图/iPhone15ProMax/iPhone15ProMax.jpg', 1);
INSERT INTO `product_image` VALUES (6, 2, '/api/upload/产品图/iPhone15ProMax/iPhone15ProMax3.jpg', 2);
INSERT INTO `product_image` VALUES (7, 3, '/api/upload/产品图/小米14Ultra/OIPCBXCqfKGoIGgwWWukpjTUszVlKCPf.jpg', 0);
INSERT INTO `product_image` VALUES (8, 3, '/api/upload/产品图/小米14Ultra/OIPCdCLZEWYrYCAnHAhduWuyEFHIezGQ.jpg', 1);
INSERT INTO `product_image` VALUES (9, 3, '/api/upload/产品图/小米14Ultra/OIPCjenouCiPuCWSsYJiUMnStnaYUtrj.jpg', 2);
INSERT INTO `product_image` VALUES (10, 3, '/api/upload/产品图/小米14Ultra/OIPCsRxitdKbGVzLXmveibGUvebhwRYS.jpg', 3);
INSERT INTO `product_image` VALUES (11, 4, '/api/upload/产品图/iPadPro12.9寸/OIPCCzPlqkvlUeWxAUppnvFlxUgYmZHC.jpg', 0);
INSERT INTO `product_image` VALUES (12, 4, '/api/upload/产品图/iPadPro12.9寸/OIPCHcLbbzaGIGWQsdMxSQeysXZfzHRE.jpg', 1);
INSERT INTO `product_image` VALUES (13, 4, '/api/upload/产品图/iPadPro12.9寸/OIPCjuoGUVjkEEdULALcIxZvEmNoKHLa.jpg', 2);
INSERT INTO `product_image` VALUES (14, 4, '/api/upload/产品图/iPadPro12.9寸/WvKaMlfWIJqxJVurSctCHFcBMKPcGsgh.jpg', 3);
INSERT INTO `product_image` VALUES (15, 5, '/api/upload/产品图/联想小新Pro16/BQBMskuQSdsVGEOmzKylKetVjeiIHwEl.jpg', 0);
INSERT INTO `product_image` VALUES (16, 5, '/api/upload/产品图/联想小新Pro16/PdYmrHGEjLeQmrOhzsPSShfLJSDXzMSL.jpg', 1);
INSERT INTO `product_image` VALUES (17, 5, '/api/upload/产品图/联想小新Pro16/RkcYefhKqsgPxKVVvjmAgjdGWLvkPXLI.jpg', 2);
INSERT INTO `product_image` VALUES (18, 5, '/api/upload/产品图/联想小新Pro16/svpWUDoqqOHZDDUigZkoEQqNfqOTZzUG.jpg', 3);
INSERT INTO `product_image` VALUES (19, 5, '/api/upload/产品图/联想小新Pro16/ZwtGrGbYdBczVgNHfoSCHKWWOxVpAHvf.jpg', 4);
INSERT INTO `product_image` VALUES (20, 6, '/api/upload/产品图/AppleWatchSeries9/LiNfmvjopqcvzuSgAAAAOedeUzCYUGTe.jpg', 0);
INSERT INTO `product_image` VALUES (21, 6, '/api/upload/产品图/AppleWatchSeries9/ndSFxWzlBtUSMYkjMymNuuiThwOlGXwN.jpg', 1);
INSERT INTO `product_image` VALUES (22, 6, '/api/upload/产品图/AppleWatchSeries9/OIPCAfvVPCUGdWdOTZyrvbVUttUVmdaR.jpg', 2);
INSERT INTO `product_image` VALUES (23, 6, '/api/upload/产品图/AppleWatchSeries9/ZHbGoKhjUZuFyYQxGQZOKMHxBfUJRJnk.jpg', 3);
INSERT INTO `product_image` VALUES (24, 7, '/api/upload/产品图/索尼WH-1000XM5耳机/bTVkAAAodtEgKSJsQJoKzPmjWcBiMXcs.jpg', 0);
INSERT INTO `product_image` VALUES (25, 7, '/api/upload/产品图/索尼WH-1000XM5耳机/OruiOvHxPZufupfspmXKZSOCrwTzaWTD.jpg', 1);
INSERT INTO `product_image` VALUES (26, 7, '/api/upload/产品图/索尼WH-1000XM5耳机/pOvoDFBZAjAydKIkTHgzNTFpMvFciZbH.jpg', 2);
INSERT INTO `product_image` VALUES (27, 7, '/api/upload/产品图/索尼WH-1000XM5耳机/zuGVeDTNONOcypxuoIynKWCGylVnWTtw.jpg', 3);
INSERT INTO `product_image` VALUES (28, 8, '/api/upload/产品图/耐克运动卫衣/jOrdkOkViLZlIRtwAIijznGiPQiMWvZQ.jpg', 0);
INSERT INTO `product_image` VALUES (29, 8, '/api/upload/产品图/耐克运动卫衣/mPYGIOPrkzULdJZzIgzFtisheQlvVvxg.jpg', 1);
INSERT INTO `product_image` VALUES (30, 8, '/api/upload/产品图/耐克运动卫衣/OIPCrkPbqgPDRIUyQpboYxPQmdUrHSiL.jpg', 2);
INSERT INTO `product_image` VALUES (31, 8, '/api/upload/产品图/耐克运动卫衣/ShpFrfRyAAAAAAAARiQeznIcqHGGzHmt.jpg', 3);
INSERT INTO `product_image` VALUES (32, 9, '/api/upload/产品图/优衣库基础款牛仔裤/OIPCBUqBzIxDnIDzIqURLODrMEXaPUmu.jpg', 0);
INSERT INTO `product_image` VALUES (33, 9, '/api/upload/产品图/优衣库基础款牛仔裤/OIPCqyzRSfMSusBThetRsSmnmuDLoplF.jpg', 1);
INSERT INTO `product_image` VALUES (34, 9, '/api/upload/产品图/优衣库基础款牛仔裤/OIPCVkoAneYJMQpuhlaxFVfLlGNgpnfN.jpg', 2);
INSERT INTO `product_image` VALUES (35, 9, '/api/upload/产品图/优衣库基础款牛仔裤/OIPCxJbeWmcyvZrivsnqXrgFIwmXSfMH.jpg', 3);
INSERT INTO `product_image` VALUES (36, 10, '/api/upload/产品图/ZARA法式连衣裙/dAudJOGkhufTzeEHEreFDAODyuAAAAAA.jpg', 0);
INSERT INTO `product_image` VALUES (37, 10, '/api/upload/产品图/ZARA法式连衣裙/OIPCVKvcKPgeHldNkGiFlOmGXpvQFuXL.jpg', 1);
INSERT INTO `product_image` VALUES (38, 10, '/api/upload/产品图/ZARA法式连衣裙/OIPCXhEUHHtkLzFzzaEscuXxkqFWADDR.jpg', 2);
INSERT INTO `product_image` VALUES (39, 10, '/api/upload/产品图/ZARA法式连衣裙/OIPCZTBfcTribkWBobFGnfMVEccVuTNv.jpg', 3);
INSERT INTO `product_image` VALUES (40, 11, '/api/upload/产品图/太平鸟雪纺衬衫/OIPCeIzrvPnJwwTzodwNIrWdhMFUoZRq.jpg', 0);
INSERT INTO `product_image` VALUES (41, 11, '/api/upload/产品图/太平鸟雪纺衬衫/OIPChxbWhuIlTKujQyKjJeqAgcAccyXp.jpg', 1);
INSERT INTO `product_image` VALUES (42, 11, '/api/upload/产品图/太平鸟雪纺衬衫/OIPCmyeOnVNEBVOzldPOuWRYfEpxzSLz.jpg', 2);
INSERT INTO `product_image` VALUES (43, 11, '/api/upload/产品图/太平鸟雪纺衬衫/OIPCzJrhtHLPIBxWvBxGgmdKInwIhbXG.jpg', 3);
INSERT INTO `product_image` VALUES (44, 12, '/api/upload/产品图/AJ1高帮板鞋/dOczMEzEcrKiLFoASqSzTmIHTpmXAAAY.jpg', 0);
INSERT INTO `product_image` VALUES (45, 12, '/api/upload/产品图/AJ1高帮板鞋/JkVVGymJpqvERnCirrZjcREOMbAWhiGH.jpg', 1);
INSERT INTO `product_image` VALUES (46, 12, '/api/upload/产品图/AJ1高帮板鞋/OIPCaRsYZzqlwZvROPkPZqxkurpUUHGe.jpg', 2);
INSERT INTO `product_image` VALUES (47, 12, '/api/upload/产品图/AJ1高帮板鞋/OIPCJnhtHukMcDtiVREiVXPCEiZIbKzu.jpg', 3);
INSERT INTO `product_image` VALUES (48, 13, '/api/upload/产品图/安踏跑步鞋/FNAcAAAAAZfQzIFhuHMOjgEeZWRIYDQZ.jpg', 0);
INSERT INTO `product_image` VALUES (49, 13, '/api/upload/产品图/安踏跑步鞋/jUHQnFsfaLoverEduUhYzpVTUxSiutmZ.jpg', 1);
INSERT INTO `product_image` VALUES (50, 13, '/api/upload/产品图/安踏跑步鞋/neoMBWBTjEJpxJvIjtNmTsKgkOVaDAxk.jpg', 2);
INSERT INTO `product_image` VALUES (51, 13, '/api/upload/产品图/安踏跑步鞋/vHyystZTKpqBZCkgtAPHSCOoOuAAmkTF.jpg', 3);
INSERT INTO `product_image` VALUES (52, 13, '/api/upload/产品图/安踏跑步鞋/WyprGktiBeaZFoSgNlGIJtxKXcgzhXME.jpg', 4);
INSERT INTO `product_image` VALUES (53, 14, '/api/upload/产品图/Coach托特包/bwAjHAQQjUoBFUQAAAASCmyBmDtNooYb.jpg', 0);
INSERT INTO `product_image` VALUES (54, 14, '/api/upload/产品图/Coach托特包/DdPvSMRiVIQirBBgsJyhFdRodfNoYZGY.jpg', 1);
INSERT INTO `product_image` VALUES (55, 14, '/api/upload/产品图/Coach托特包/gMCyEjpROXIQGQAAnIJDqDpRjyIWXTCT.jpg', 2);
INSERT INTO `product_image` VALUES (56, 14, '/api/upload/产品图/Coach托特包/NwhjJasNgRaCcBzKAAAcWUqwWUrKyQIZ.jpg', 3);
INSERT INTO `product_image` VALUES (57, 14, '/api/upload/产品图/Coach托特包/PquGRaUbNQmfBCaAAAAJpCwjMtYzvHDS.jpg', 4);
INSERT INTO `product_image` VALUES (58, 14, '/api/upload/产品图/Coach托特包/ujNbnyJqAQAAAAkivikSgsiIiWuJEuDX.jpg', 5);
INSERT INTO `product_image` VALUES (59, 15, '/api/upload/产品图/施华洛世奇项链/OIPCBSLcVpNkbXxeKyLmPcaTemmvNAAC.jpg', 0);
INSERT INTO `product_image` VALUES (60, 15, '/api/upload/产品图/施华洛世奇项链/OIPChIhLBHhpnyuzFebSyXHNfSOXpRTb.jpg', 1);
INSERT INTO `product_image` VALUES (61, 15, '/api/upload/产品图/施华洛世奇项链/OIPCHvNxzZQUIwxyqChzkqTZgwvZVSzu.jpg', 2);
INSERT INTO `product_image` VALUES (62, 15, '/api/upload/产品图/施华洛世奇项链/OIPCwrjBNkRRFKiSZoWAppoTVHtUHXRG.jpg', 3);
INSERT INTO `product_image` VALUES (63, 15, '/api/upload/产品图/施华洛世奇项链/sxwAAAAAAAAbbqqZMooRkjvALxplsdib.jpg', 4);
INSERT INTO `product_image` VALUES (64, 16, '/api/upload/产品图/三只松鼠坚果礼盒/BAAAACOnOEMQRgBBicYFrpsLgvBSqStB.jpg', 0);
INSERT INTO `product_image` VALUES (65, 16, '/api/upload/产品图/三只松鼠坚果礼盒/MAAAAAVqqeszWxbCIeaJAsmmkTeliMYo.jpg', 1);
INSERT INTO `product_image` VALUES (66, 16, '/api/upload/产品图/三只松鼠坚果礼盒/OIPCehhAKdeONdlSutqnXKVVjaEPxSLU.jpg', 2);
INSERT INTO `product_image` VALUES (67, 16, '/api/upload/产品图/三只松鼠坚果礼盒/OIPCiWMTJRmDynjpggULnQItDhvVBtfp.jpg', 3);
INSERT INTO `product_image` VALUES (68, 16, '/api/upload/产品图/三只松鼠坚果礼盒/raFybVMADAEBAAAAAAAAAAAAgrCJdPKg.jpg', 4);
INSERT INTO `product_image` VALUES (69, 17, '/api/upload/产品图/百草味夏威夷果/hLriuljASAAvgMAAAAwoVDtOMINWgaaf.jpg', 0);
INSERT INTO `product_image` VALUES (70, 17, '/api/upload/产品图/百草味夏威夷果/LqZSTqCqlpOSaAmLXmytGjAAAEZowAAB.jpg', 1);
INSERT INTO `product_image` VALUES (71, 17, '/api/upload/产品图/百草味夏威夷果/PiNBIqKngcewBLVXfaKfmACOwAAAAAAA.jpg', 2);
INSERT INTO `product_image` VALUES (72, 17, '/api/upload/产品图/百草味夏威夷果/TwSorGMSTiFOVQgSWlEAXVxZRsEXxdRm.jpg', 3);
INSERT INTO `product_image` VALUES (73, 17, '/api/upload/产品图/百草味夏威夷果/ZBpOepUiNmiwIvgAAAUwAAAAAAAIiwjf.jpg', 4);
INSERT INTO `product_image` VALUES (74, 18, '/api/upload/产品图/奥利奥夹心饼干/nsoeizXudMncSbIJSfDSVUmSaTCSVkTF.jpg', 0);
INSERT INTO `product_image` VALUES (75, 18, '/api/upload/产品图/奥利奥夹心饼干/OIPCzXNCHmSkjchWyEOuLgzrKyWEhCYm.jpg', 1);
INSERT INTO `product_image` VALUES (76, 18, '/api/upload/产品图/奥利奥夹心饼干/pMuboxSHLsFPxAolVsPMAAAAAffodZdR.jpg', 2);
INSERT INTO `product_image` VALUES (77, 18, '/api/upload/产品图/奥利奥夹心饼干/SzNNSzNCKNCJokAjIoKHTyHQvvLGZVLO.jpg', 3);
INSERT INTO `product_image` VALUES (78, 18, '/api/upload/产品图/奥利奥夹心饼干/XPWzFIcgGLNunvVgXqhWDWSBueBXETpH.jpg', 4);
INSERT INTO `product_image` VALUES (79, 19, '/api/upload/产品图/良品铺子猪肉脯/FgCJAAAIGYKsXMNBHGoDNUhbkwlICvfB.jpg', 0);
INSERT INTO `product_image` VALUES (80, 19, '/api/upload/产品图/良品铺子猪肉脯/OIPCbarHjEMAefjGLLNeuvEmRffsgZdc.jpg', 1);
INSERT INTO `product_image` VALUES (81, 19, '/api/upload/产品图/良品铺子猪肉脯/OIPCJMYBekaQUYEZCKQNPrDOuOXGjkTf.jpg', 2);
INSERT INTO `product_image` VALUES (82, 19, '/api/upload/产品图/良品铺子猪肉脯/SLNKvboHnjbEEnjOtqjbgtaUhfezCTKv.jpg', 3);
INSERT INTO `product_image` VALUES (83, 19, '/api/upload/产品图/良品铺子猪肉脯/wTBDteUYyGcifBCJshBaAArSfUoAFVoB.jpg', 4);
INSERT INTO `product_image` VALUES (84, 20, '/api/upload/产品图/元气森林气泡水/GgGeUamasSbLvuYAiRFKGUvjeAAAAAAA.jpg', 0);
INSERT INTO `product_image` VALUES (85, 20, '/api/upload/产品图/元气森林气泡水/LRifJLIgDrbaOoXAwoQPjagAAAhDBKNX.jpg', 1);
INSERT INTO `product_image` VALUES (86, 20, '/api/upload/产品图/元气森林气泡水/OIPCwHUMTDOPZETrgZegqSszOBJQvRWu.jpg', 2);
INSERT INTO `product_image` VALUES (87, 20, '/api/upload/产品图/元气森林气泡水/pOevoppuohvrapeHfSQLCGeNpddxcHEe.jpg', 3);
INSERT INTO `product_image` VALUES (88, 20, '/api/upload/产品图/元气森林气泡水/vtpfzHmYljopTFGQgrfZRTrHgAAAAcmZ.jpg', 4);
INSERT INTO `product_image` VALUES (89, 21, '/api/upload/产品图/康师傅红烧牛肉面/aHWtgByjaUfvETxucUEnizCIAuPEDuDG.jpg', 0);
INSERT INTO `product_image` VALUES (90, 21, '/api/upload/产品图/康师傅红烧牛肉面/GvYMSrNPEYAClrxJltNBJAAAbNuIovNb.jpg', 1);
INSERT INTO `product_image` VALUES (91, 21, '/api/upload/产品图/康师傅红烧牛肉面/JksnOJxDGelVQCHawDbRIHgAAAAavcMY.jpg', 2);
INSERT INTO `product_image` VALUES (92, 21, '/api/upload/产品图/康师傅红烧牛肉面/QMuRVLuIAFunDFHKFwAGIVIUAAAALMUA.jpg', 3);
INSERT INTO `product_image` VALUES (93, 21, '/api/upload/产品图/康师傅红烧牛肉面/rPgasqrMYFbAAAAAAAAAOtRMTIayDqgT.jpg', 4);
INSERT INTO `product_image` VALUES (94, 22, '/api/upload/产品图/美的电饭煲/LjzAAAAAAAAAArYWcxJHLSKtjkTTJuOS.jpg', 0);
INSERT INTO `product_image` VALUES (95, 22, '/api/upload/产品图/美的电饭煲/OIPCBvgMHBQwVBnrcOlJQbhrMEOggrDg.jpg', 1);
INSERT INTO `product_image` VALUES (96, 22, '/api/upload/产品图/美的电饭煲/OIPCmyXPpgTtkeITEDbupzEbgRiGxYDX.jpg', 2);
INSERT INTO `product_image` VALUES (97, 22, '/api/upload/产品图/美的电饭煲/uhQiOTzUyFfjjjhggPuHtDPfXrLDTIYs.jpg', 3);
INSERT INTO `product_image` VALUES (98, 22, '/api/upload/产品图/美的电饭煲/xWtHyDXyFMQZdTsKQVhisStoDqwAAAAA.jpg', 4);
INSERT INTO `product_image` VALUES (99, 23, '/api/upload/产品图/苏泊尔空气炸锅/DwotzsNIxRJmdrljzAIHnACGbgsMNffv.jpg', 0);
INSERT INTO `product_image` VALUES (100, 23, '/api/upload/产品图/苏泊尔空气炸锅/ENJGygAIYjbgOjwTUWACxyYPXvcaiSGa.jpg', 1);
INSERT INTO `product_image` VALUES (101, 23, '/api/upload/产品图/苏泊尔空气炸锅/hGpChnJhLegTLXEdkRPMAAAyFaJCjyrc.jpg', 2);
INSERT INTO `product_image` VALUES (102, 23, '/api/upload/产品图/苏泊尔空气炸锅/jEKREwNwVedCWeQCCMWmGukaSPwtYSCg.jpg', 3);
INSERT INTO `product_image` VALUES (103, 23, '/api/upload/产品图/苏泊尔空气炸锅/pqpAFUvbUyWqHajHyduGcALlCAAABhAA.jpg', 4);
INSERT INTO `product_image` VALUES (104, 24, '/api/upload/产品图/戴森吹风机/HodqQWAnURuXKDzgpGCmTqCBzWuSDddx.jpg', 0);
INSERT INTO `product_image` VALUES (105, 24, '/api/upload/产品图/戴森吹风机/rQkdoBBwYsaLdgbpcEYuMhJUAAAxlAsu.jpg', 1);
INSERT INTO `product_image` VALUES (106, 24, '/api/upload/产品图/戴森吹风机/ryFubHWBFBvbLqWmpFKYZcMBIlosFHbU.jpg', 2);
INSERT INTO `product_image` VALUES (107, 24, '/api/upload/产品图/戴森吹风机/XQuLPQoWGhPkcXmbsEjQAwNhStiugIIH.jpg', 3);
INSERT INTO `product_image` VALUES (108, 24, '/api/upload/产品图/戴森吹风机/xTGmfESssAkAAAAAZeecSaLdFypSaCfm.jpg', 4);
INSERT INTO `product_image` VALUES (109, 25, '/api/upload/产品图/小米扫地机器人/bLCttkbhWwUACZmuetMptIAAVdkoyQzp.jpg', 0);
INSERT INTO `product_image` VALUES (110, 25, '/api/upload/产品图/小米扫地机器人/DhaoYcDNNiuICCNfgAAAALOCTLMUSGtQ.jpg', 1);
INSERT INTO `product_image` VALUES (111, 25, '/api/upload/产品图/小米扫地机器人/LHTwKcSQqkpjgbKKjdrWriiRnepjEPAV.jpg', 2);
INSERT INTO `product_image` VALUES (112, 25, '/api/upload/产品图/小米扫地机器人/OIPCDsGKDIwjIASsdRaLczmCQjlMpXXc.jpg', 3);
INSERT INTO `product_image` VALUES (113, 25, '/api/upload/产品图/小米扫地机器人/OIPCJOQTMgjJKQZTNIFYLLADrILwPAul.jpg', 4);
INSERT INTO `product_image` VALUES (114, 25, '/api/upload/产品图/小米扫地机器人/QFvjKJNJOKUIzGkbKbKEFUjEpPjowJpW.jpg', 5);
INSERT INTO `product_image` VALUES (115, 26, '/api/upload/产品图/飞利浦电动牙刷/AXPBHxQsGWjAXQChBVbiDhVAAAAAJDMP.jpg', 0);
INSERT INTO `product_image` VALUES (116, 26, '/api/upload/产品图/飞利浦电动牙刷/OIPCaFhDGdYxrCukYjnYdUmAxXLQmcSN.jpg', 1);
INSERT INTO `product_image` VALUES (117, 26, '/api/upload/产品图/飞利浦电动牙刷/OIPCduPfIcvvYiKMugJJQPOtRgTOBFWI.jpg', 2);
INSERT INTO `product_image` VALUES (118, 26, '/api/upload/产品图/飞利浦电动牙刷/OIPCWQbrKptVcqxIrKNZKEvFtfVaWNaO.jpg', 3);
INSERT INTO `product_image` VALUES (119, 26, '/api/upload/产品图/飞利浦电动牙刷/QRUXJSOBDIFHBwjAbBkzVSHjTQbPyTUC.jpg', 4);
INSERT INTO `product_image` VALUES (120, 26, '/api/upload/产品图/飞利浦电动牙刷/XWCDLipbYLDiLQgLPreMhvgazfCiNiCw.jpg', 5);
INSERT INTO `product_image` VALUES (121, 27, '/api/upload/产品图/海尔冰箱双开门/bgXCBaxsVZfFrOLsgNGgAAAASNwuzRRT.jpg', 0);
INSERT INTO `product_image` VALUES (122, 27, '/api/upload/产品图/海尔冰箱双开门/dWyeoYyKAVSABCIAAAAAJUwETnoWCGqr.jpg', 1);
INSERT INTO `product_image` VALUES (123, 27, '/api/upload/产品图/海尔冰箱双开门/IKwgkoyAAAAgsRuChZzsGKwqWualJDxH.jpg', 2);
INSERT INTO `product_image` VALUES (124, 27, '/api/upload/产品图/海尔冰箱双开门/JaYCJgstiNoqOawAADFvyZPEtxvRrSxl.jpg', 3);
INSERT INTO `product_image` VALUES (125, 27, '/api/upload/产品图/海尔冰箱双开门/jxdSBeAAAAAAANhNHAJIQrQNTHgFkyLU.jpg', 4);
INSERT INTO `product_image` VALUES (126, 28, '/api/upload/产品图/补水面膜/BkcHccOuVTetiKABTvQbDgPouAUKNdAQ.jpg', 0);
INSERT INTO `product_image` VALUES (127, 28, '/api/upload/产品图/补水面膜/gXIzxasbGudAHbQAAATtoXoPIDlhhgnA.jpg', 1);
INSERT INTO `product_image` VALUES (128, 28, '/api/upload/产品图/补水面膜/kzlIkcpPfTOXiSoZrxLRDqsWkqLPXzvA.jpg', 2);
INSERT INTO `product_image` VALUES (129, 28, '/api/upload/产品图/补水面膜/lKGpCUJvIAAAAFpJSIesSBfjguZlZfnE.jpg', 3);
INSERT INTO `product_image` VALUES (130, 28, '/api/upload/产品图/补水面膜/SaEVVQgTKydNuxCqpCpJaVMwUsHRNmEw.jpg', 4);
INSERT INTO `product_image` VALUES (131, 28, '/api/upload/产品图/补水面膜/sgOKnJgXSryrvlqgvtkseuNZpQEwzuGn.jpg', 5);
INSERT INTO `product_image` VALUES (132, 29, '/api/upload/产品图/SK-II神仙水/DnoyjJarLHmYeubIwqSjiiigDCSqVHyQ.jpg', 0);
INSERT INTO `product_image` VALUES (133, 29, '/api/upload/产品图/SK-II神仙水/DTwUAmfaPAAAAPVBXhUNUubpvGhstort.jpg', 1);
INSERT INTO `product_image` VALUES (134, 29, '/api/upload/产品图/SK-II神仙水/eAfblCGESBvDtXChISlFreRfSzfBwqnZ.jpg', 2);
INSERT INTO `product_image` VALUES (135, 29, '/api/upload/产品图/SK-II神仙水/OIPCOiZEAeyVajodGJlfYLLkhhUpsjoA.jpg', 3);
INSERT INTO `product_image` VALUES (136, 29, '/api/upload/产品图/SK-II神仙水/uLKdBhdqAAALomuXkAuVYsVsImcstscf.jpg', 4);
INSERT INTO `product_image` VALUES (137, 29, '/api/upload/产品图/SK-II神仙水/ZPGAAAAIQvdcdGMasxltrJkvJQeKSGWI.jpg', 5);
INSERT INTO `product_image` VALUES (138, 30, '/api/upload/产品图/YSL小金条口红/BTvWAHRIANNpqmSMSoPGFKRJYFgMeNdW.jpg', 0);
INSERT INTO `product_image` VALUES (139, 30, '/api/upload/产品图/YSL小金条口红/dYhmWukhXWkMHGwGfZpbOoRVoDAtkllz.jpg', 1);
INSERT INTO `product_image` VALUES (140, 30, '/api/upload/产品图/YSL小金条口红/gAACjWOlamHESxEaRkDCVfgDzXwgEKot.jpg', 2);
INSERT INTO `product_image` VALUES (141, 30, '/api/upload/产品图/YSL小金条口红/glAAFGuiYGVQkZoAiguJAgitwkGfXHsf.jpg', 3);
INSERT INTO `product_image` VALUES (142, 30, '/api/upload/产品图/YSL小金条口红/RKuhUPmElTpnjMYLBtVKMbVkDLJaAxfg.jpg', 4);
INSERT INTO `product_image` VALUES (143, 31, '/api/upload/产品图/迪奥999口红/DEnBNgMNQWnPBawabxlApIdKqctNjXsB.jpg', 0);
INSERT INTO `product_image` VALUES (144, 31, '/api/upload/产品图/迪奥999口红/dVrLRHbQrToKRFAYACAuxMwBGuAAAAAA.jpg', 1);
INSERT INTO `product_image` VALUES (145, 31, '/api/upload/产品图/迪奥999口红/OIPCgORAESMuZcxvLUQXxzwSHriEhxAU.jpg', 2);
INSERT INTO `product_image` VALUES (146, 31, '/api/upload/产品图/迪奥999口红/pEEzXLbXKUCgJHSreCcJOnqdPkAAAAAA.jpg', 3);
INSERT INTO `product_image` VALUES (147, 31, '/api/upload/产品图/迪奥999口红/WBzwDjnqzRAAnXZGQNMzIXUEKQAatYQm.jpg', 4);
INSERT INTO `product_image` VALUES (148, 32, '/api/upload/产品图/香奈儿邂逅香水/OIPCijOsvBeBkNCdPxdynvFAVZIXxEwh.jpg', 0);
INSERT INTO `product_image` VALUES (149, 32, '/api/upload/产品图/香奈儿邂逅香水/OIPCVSzOclOUvlKrVpPIdzkzZPnyzyVp.jpg', 1);
INSERT INTO `product_image` VALUES (150, 32, '/api/upload/产品图/香奈儿邂逅香水/OIPCzMycKGpbVrqdoeadBgxDjrCLbBeX.jpg', 2);
INSERT INTO `product_image` VALUES (151, 32, '/api/upload/产品图/香奈儿邂逅香水/OPsppjKuZfkWjqPZewALmBygAAAwzDfD.jpg', 3);
INSERT INTO `product_image` VALUES (152, 32, '/api/upload/产品图/香奈儿邂逅香水/SahbMWcOaKTsBUrdnjjdFjdEurTZNXEo.jpg', 4);
INSERT INTO `product_image` VALUES (153, 33, '/api/upload/产品图/舒肤佳沐浴露/DokzuAsvgtvryTfHQySYgAAAMcjsIkaV.jpg', 0);
INSERT INTO `product_image` VALUES (154, 33, '/api/upload/产品图/舒肤佳沐浴露/eQZDVhrtJDhsqFDLiggZXYpPAKUKGYjQ.jpg', 1);
INSERT INTO `product_image` VALUES (155, 33, '/api/upload/产品图/舒肤佳沐浴露/FzYzuPjWzlZjUscoHCJeoAAACqCHAAAA.jpg', 2);
INSERT INTO `product_image` VALUES (156, 33, '/api/upload/产品图/舒肤佳沐浴露/OIPCjzpUrNyOUUARnweeJQdxhPZUOHAa.jpg', 3);
INSERT INTO `product_image` VALUES (157, 33, '/api/upload/产品图/舒肤佳沐浴露/yThyGFGatHmDsbDdxbmRdFXawAszfMoO.jpg', 4);
INSERT INTO `product_image` VALUES (158, 33, '/api/upload/产品图/舒肤佳沐浴露/znwAAAAqjmuLFYqUYQXElfojiFyiKMkS.jpg', 5);
INSERT INTO `product_image` VALUES (159, 34, '/api/upload/产品图/植村秀化妆刷套装/JHhMSQwAAAAAAAOPAzAtRrkDWWmklRfY.jpg', 0);
INSERT INTO `product_image` VALUES (160, 34, '/api/upload/产品图/植村秀化妆刷套装/OIPCBvqFnsRzRyUZxmRAkTrAfhGqUjwr.jpg', 1);
INSERT INTO `product_image` VALUES (161, 34, '/api/upload/产品图/植村秀化妆刷套装/OIPCCLnEoGdzfYbRrVJpwVlmrLkTtdic.jpg', 2);
INSERT INTO `product_image` VALUES (162, 34, '/api/upload/产品图/植村秀化妆刷套装/OIPCzFguCmaLxeDAqJnIZxlQeOBGBgYv.jpg', 3);
INSERT INTO `product_image` VALUES (163, 34, '/api/upload/产品图/植村秀化妆刷套装/sawsIfDxYXLkcLhOxZHdcgcVQPTcaiVa.jpg', 4);
INSERT INTO `product_image` VALUES (164, 34, '/api/upload/产品图/植村秀化妆刷套装/xFHdvMizOLliuszWPjDZFQBnKCqPitjc.jpg', 5);
INSERT INTO `product_image` VALUES (165, 35, '/api/upload/产品图/无线蓝牙耳机AirPro/FJAANyAAAAActQsccIcOoxWADGGubmSB.jpg', 0);
INSERT INTO `product_image` VALUES (166, 35, '/api/upload/产品图/无线蓝牙耳机AirPro/oyAWVYdTzASOjcyBrLHxaQwrBpWNAxxL.jpg', 1);
INSERT INTO `product_image` VALUES (167, 35, '/api/upload/产品图/无线蓝牙耳机AirPro/RItvzuPAApFEOYWGaaMjhvWMurXfOUVu.jpg', 2);
INSERT INTO `product_image` VALUES (168, 35, '/api/upload/产品图/无线蓝牙耳机AirPro/UeUxhewAazcGuDxwxMtlXiHtmMgqGWoy.jpg', 3);
INSERT INTO `product_image` VALUES (169, 35, '/api/upload/产品图/无线蓝牙耳机AirPro/vtYyIQfdQQBAAAAejcBjrkdUJyLoRukB.jpg', 4);
INSERT INTO `product_image` VALUES (170, 36, '/api/upload/产品图/20000mAh快充充电宝/fEaSFdgAAAnKkYcQpvtQqpUvqKedkgzU.jpg', 0);
INSERT INTO `product_image` VALUES (171, 36, '/api/upload/产品图/20000mAh快充充电宝/kOyIZZoomlgqeUOTcDBJwAAAAoeWPiLL.jpg', 1);
INSERT INTO `product_image` VALUES (172, 36, '/api/upload/产品图/20000mAh快充充电宝/mgzUEgBJRzWqDxRlHTLyeuFJPGPATphy.jpg', 2);
INSERT INTO `product_image` VALUES (173, 36, '/api/upload/产品图/20000mAh快充充电宝/STYepxgdpyFqQcqxqtkpNjCbbmarcEXs.jpg', 3);
INSERT INTO `product_image` VALUES (174, 36, '/api/upload/产品图/20000mAh快充充电宝/XgRnxCFdLVFcAAcZvtbpLOFdlsuZrvzn.jpg', 4);
INSERT INTO `product_image` VALUES (175, 37, '/api/upload/产品图/Type-C7合1扩展坞/AHqVnhXPgCoJSEubwAAAASXDIAqQVonZ.jpg', 0);
INSERT INTO `product_image` VALUES (176, 37, '/api/upload/产品图/Type-C7合1扩展坞/DJKIfNoEkAouVIglzvOXxkdGLwgQIZVq.jpg', 1);
INSERT INTO `product_image` VALUES (177, 37, '/api/upload/产品图/Type-C7合1扩展坞/kdWGmRqTRixKFdDVxNQnzqHCuntVxFUt.jpg', 2);
INSERT INTO `product_image` VALUES (178, 37, '/api/upload/产品图/Type-C7合1扩展坞/LiJcAAAALpMkjPYTiOmfZHJhGdPybJlW.jpg', 3);
INSERT INTO `product_image` VALUES (179, 37, '/api/upload/产品图/Type-C7合1扩展坞/YqzKzBmeILPWgvkDongymbHIOdvKgIKA.jpg', 4);
INSERT INTO `product_image` VALUES (180, 38, '/api/upload/产品图/RGB机械键盘青轴/dAXZzcDFinSBHpePTqBjYcwCmHIkaCpV.jpg', 0);
INSERT INTO `product_image` VALUES (181, 38, '/api/upload/产品图/RGB机械键盘青轴/hNXpwbxDutWMnoAFVMwAAAclukkfHOSg.jpg', 1);
INSERT INTO `product_image` VALUES (182, 38, '/api/upload/产品图/RGB机械键盘青轴/LxFfsUpdPvoYRQiZCAulLLYMzQpffgOK.jpg', 2);
INSERT INTO `product_image` VALUES (183, 38, '/api/upload/产品图/RGB机械键盘青轴/MmnAWInZhhTYwAAgkVkEsbKbBEfmXiTZ.jpg', 3);
INSERT INTO `product_image` VALUES (184, 38, '/api/upload/产品图/RGB机械键盘青轴/nwjQnbofTpWwTYtdhFNlHZYaNGPnPqlc.jpg', 4);
INSERT INTO `product_image` VALUES (185, 38, '/api/upload/产品图/RGB机械键盘青轴/tjtPCKAFuMNEmbiGOtxsChnlQKLwgAAT.jpg', 5);
INSERT INTO `product_image` VALUES (186, 39, '/api/upload/产品图/静音无线鼠标/IFFbFxbgZyopJKRHABqRAEcKAAAAAwAA.jpg', 0);
INSERT INTO `product_image` VALUES (187, 39, '/api/upload/产品图/静音无线鼠标/nWzZotPXpbJnojXOPipcwlSOOdNfvOQH.jpg', 1);
INSERT INTO `product_image` VALUES (188, 39, '/api/upload/产品图/静音无线鼠标/QswnzvbXIAAyeBAfqEDlSiDWKNaxnPue.jpg', 2);
INSERT INTO `product_image` VALUES (189, 39, '/api/upload/产品图/静音无线鼠标/SvKNHJhdRPqTPWaKHZXbmnZvVwLovCxq.jpg', 3);
INSERT INTO `product_image` VALUES (190, 39, '/api/upload/产品图/静音无线鼠标/uABSPgCqmVgZVmdBJIgXpSDBpPJWtpiW.jpg', 4);
INSERT INTO `product_image` VALUES (191, 40, '/api/upload/产品图/手机直播支架落地款/aslDCUmQoHniWBkMXuimCHzREaXqBkrI.jpg', 0);
INSERT INTO `product_image` VALUES (192, 40, '/api/upload/产品图/手机直播支架落地款/EChnkxgqZWlFEyHixpFpibgAEkzdymES.jpg', 1);
INSERT INTO `product_image` VALUES (193, 40, '/api/upload/产品图/手机直播支架落地款/IZRWaduBaBZSAAjZHHcNcgMUusBLfYDG.jpg', 2);
INSERT INTO `product_image` VALUES (194, 40, '/api/upload/产品图/手机直播支架落地款/okAAAAbsoOQIcaXkAveXILQSxQRjnIHA.jpg', 3);
INSERT INTO `product_image` VALUES (195, 40, '/api/upload/产品图/手机直播支架落地款/xIodCjYAAdnmcawMfAjzQHjyEYKOfaHQ.jpg', 4);
INSERT INTO `product_image` VALUES (196, 40, '/api/upload/产品图/手机直播支架落地款/xlgCIGGaOYAAAlVuHnaVFjQAErJREMeV.jpg', 5);
INSERT INTO `product_image` VALUES (197, 41, '/api/upload/产品图/三合一快充数据线/nFwAAGJSWlzHoTlnwMygFLBBUaOedyrm.jpg', 0);
INSERT INTO `product_image` VALUES (198, 41, '/api/upload/产品图/三合一快充数据线/OIPCZOrAGFsrWOssmukARQWWWuVEYsdm.jpg', 1);
INSERT INTO `product_image` VALUES (199, 41, '/api/upload/产品图/三合一快充数据线/pKnUvAGFQxQLYwzswpFaeAPjTNBhZpkM.jpg', 2);
INSERT INTO `product_image` VALUES (200, 41, '/api/upload/产品图/三合一快充数据线/VGbzucMEArXuQOkAjzuURnLyxIDgdtMD.jpg', 3);
INSERT INTO `product_image` VALUES (201, 41, '/api/upload/产品图/三合一快充数据线/vIKveMltOxvqSoPzhUdfWhasshsdsLBp.jpg', 4);
INSERT INTO `product_image` VALUES (202, 41, '/api/upload/产品图/三合一快充数据线/zLTbmAGOzXBmQAAAJFaCuIyAsXFqlHlh.jpg', 5);
INSERT INTO `product_image` VALUES (203, 42, '/api/upload/产品图/桌面USB小风扇/hmEozRCfylgiWYHiAAAAAAiCfBSsvynP.jpg', 0);
INSERT INTO `product_image` VALUES (204, 42, '/api/upload/产品图/桌面USB小风扇/HvxefJDfuuDeVsEpDyEYuSgAAApxjNIH.jpg', 1);
INSERT INTO `product_image` VALUES (205, 42, '/api/upload/产品图/桌面USB小风扇/jLakNeSKdigAiIjJeWcCOHZfEYWEOABB.jpg', 2);
INSERT INTO `product_image` VALUES (206, 42, '/api/upload/产品图/桌面USB小风扇/OIPCFaVzkviutkHREAnUkZnHaTwxMuGl.jpg', 3);
INSERT INTO `product_image` VALUES (207, 42, '/api/upload/产品图/桌面USB小风扇/OIPCPspjKAUOlBDCtAaOUskpVFTrJAGO.jpg', 4);
INSERT INTO `product_image` VALUES (208, 42, '/api/upload/产品图/桌面USB小风扇/OIPCshcZapBvVFUZATRHGZOsvNhWSTwR.jpg', 5);

-- ----------------------------
-- Table structure for product_sku
-- ----------------------------
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品id',
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规格 红-L',
  `price` decimal(10, 2) NOT NULL COMMENT '规格单价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '规格库存',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pid`(`product_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品SKU规格' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_sku
-- ----------------------------
INSERT INTO `product_sku` VALUES (1, 1, '黑色 12+256G', 6299.00, 29);
INSERT INTO `product_sku` VALUES (2, 1, '白色 12+512G', 6799.00, 20);
INSERT INTO `product_sku` VALUES (3, 2, '256G 黑色', 9999.00, 30);
INSERT INTO `product_sku` VALUES (4, 2, '512G 白色', 11999.00, 20);
INSERT INTO `product_sku` VALUES (5, 5, '16G+512G', 4999.00, 59);
INSERT INTO `product_sku` VALUES (6, 8, 'M码 黑色', 269.00, 100);
INSERT INTO `product_sku` VALUES (7, 8, 'L码 白色', 269.00, 80);
INSERT INTO `product_sku` VALUES (8, 10, 'S码 米色', 399.00, 50);
INSERT INTO `product_sku` VALUES (9, 10, 'M码 黑色', 399.00, 60);
INSERT INTO `product_sku` VALUES (10, 11, '均码 白色', 259.00, 70);
INSERT INTO `product_sku` VALUES (11, 12, '42码 黑白', 1299.00, 40);
INSERT INTO `product_sku` VALUES (12, 12, '43码 黑白', 1299.00, 35);
INSERT INTO `product_sku` VALUES (13, 13, '42码 黑色', 399.00, 90);
INSERT INTO `product_sku` VALUES (14, 16, '1kg装', 79.90, 200);
INSERT INTO `product_sku` VALUES (15, 22, '4L容量', 189.00, 80);
INSERT INTO `product_sku` VALUES (16, 23, '标准款', 299.00, 60);
INSERT INTO `product_sku` VALUES (17, 24, '紫色款', 2990.00, 27);
INSERT INTO `product_sku` VALUES (18, 25, '标准款', 1999.00, 39);
INSERT INTO `product_sku` VALUES (19, 28, '单片装5片', 69.90, 200);
INSERT INTO `product_sku` VALUES (20, 28, '十片装', 129.00, 150);
INSERT INTO `product_sku` VALUES (21, 29, '230ml', 1590.00, 29);
INSERT INTO `product_sku` VALUES (22, 30, '#21 复古红', 380.00, 100);
INSERT INTO `product_sku` VALUES (23, 31, '#999正红色', 350.00, 100);
INSERT INTO `product_sku` VALUES (24, 32, '50ml', 1090.00, 40);
INSERT INTO `product_sku` VALUES (25, 35, '标准版 白色', 199.00, 147);
INSERT INTO `product_sku` VALUES (26, 35, '标准版 黑色', 199.00, 120);
INSERT INTO `product_sku` VALUES (27, 36, '20000mAh 白色', 89.00, 192);
INSERT INTO `product_sku` VALUES (28, 36, '20000mAh 黑色', 89.00, 180);
INSERT INTO `product_sku` VALUES (29, 37, '7合1 深空灰', 149.00, 80);
INSERT INTO `product_sku` VALUES (30, 38, '87键 青轴 黑色', 299.00, 53);
INSERT INTO `product_sku` VALUES (31, 39, '双模 黑色', 79.00, 99);
INSERT INTO `product_sku` VALUES (32, 39, '双模 白色', 79.00, 100);
INSERT INTO `product_sku` VALUES (33, 40, '落地款 黑色', 25.00, 300);
INSERT INTO `product_sku` VALUES (34, 41, '1.2米 黑色', 35.00, 498);
INSERT INTO `product_sku` VALUES (35, 42, '标准款 白色', 59.00, 184);
INSERT INTO `product_sku` VALUES (36, 5, '16G+1TB', 5499.00, 40);
INSERT INTO `product_sku` VALUES (37, 3, '黑色 12+256G', 5999.00, 50);
INSERT INTO `product_sku` VALUES (38, 3, '白色 16+512G', 6499.00, 27);
INSERT INTO `product_sku` VALUES (39, 4, '银色 256G', 8999.00, 25);
INSERT INTO `product_sku` VALUES (40, 4, '深空灰 512G', 10499.00, 15);
INSERT INTO `product_sku` VALUES (41, 6, '午夜色 41mm', 2999.00, 40);
INSERT INTO `product_sku` VALUES (42, 6, '星光色 45mm', 3299.00, 35);
INSERT INTO `product_sku` VALUES (43, 7, '黑色', 2499.00, 30);
INSERT INTO `product_sku` VALUES (44, 7, '铂金银', 2499.00, 25);
INSERT INTO `product_sku` VALUES (45, 9, '28码 深蓝', 199.00, 54);
INSERT INTO `product_sku` VALUES (46, 9, '30码 黑色', 199.00, 75);
INSERT INTO `product_sku` VALUES (47, 9, '32码 深蓝', 199.00, 50);
INSERT INTO `product_sku` VALUES (48, 14, '棕色 大号', 2999.00, 20);
INSERT INTO `product_sku` VALUES (49, 14, '黑色 中号', 2899.00, 25);
INSERT INTO `product_sku` VALUES (50, 15, '银色 经典款', 899.00, 39);
INSERT INTO `product_sku` VALUES (51, 15, '金色 经典款', 899.00, 35);
INSERT INTO `product_sku` VALUES (52, 17, '200g装', 29.90, 200);
INSERT INTO `product_sku` VALUES (53, 17, '500g装', 49.90, 150);
INSERT INTO `product_sku` VALUES (54, 18, '原味 97g', 19.90, 299);
INSERT INTO `product_sku` VALUES (55, 18, '巧克力味 97g', 19.90, 280);
INSERT INTO `product_sku` VALUES (56, 19, '原味 200g', 39.90, 179);
INSERT INTO `product_sku` VALUES (57, 19, '蜜汁味 200g', 39.90, 170);
INSERT INTO `product_sku` VALUES (58, 20, '白桃味 480ml', 5.90, 497);
INSERT INTO `product_sku` VALUES (59, 20, '青瓜味 480ml', 5.90, 499);
INSERT INTO `product_sku` VALUES (60, 21, '桶装', 3.50, 600);
INSERT INTO `product_sku` VALUES (61, 21, '袋装', 2.50, 800);
INSERT INTO `product_sku` VALUES (62, 26, '标准款 HX6730', 399.00, 45);
INSERT INTO `product_sku` VALUES (63, 26, '高端款 HX9352', 599.00, 39);
INSERT INTO `product_sku` VALUES (64, 27, '470L 银色', 3999.00, 20);
INSERT INTO `product_sku` VALUES (65, 27, '520L 金色', 4599.00, 15);
INSERT INTO `product_sku` VALUES (66, 29, '75ml', 1590.00, 40);
INSERT INTO `product_sku` VALUES (67, 29, '230ml', 2990.00, 20);
INSERT INTO `product_sku` VALUES (68, 30, '#21 正红色', 380.00, 50);
INSERT INTO `product_sku` VALUES (69, 30, '#1966 红棕色', 380.00, 45);
INSERT INTO `product_sku` VALUES (70, 31, '哑光版', 350.00, 55);
INSERT INTO `product_sku` VALUES (71, 31, '滋润版', 350.00, 50);
INSERT INTO `product_sku` VALUES (72, 32, '50ml', 1090.00, 25);
INSERT INTO `product_sku` VALUES (73, 32, '100ml', 1590.00, 15);
INSERT INTO `product_sku` VALUES (74, 33, '400ml', 29.90, 200);
INSERT INTO `product_sku` VALUES (75, 33, '720ml', 39.90, 150);
INSERT INTO `product_sku` VALUES (76, 34, '基础5件套', 199.00, 80);
INSERT INTO `product_sku` VALUES (77, 34, '专业12件套', 399.00, 40);
INSERT INTO `product_sku` VALUES (78, 35, '标准版 白色', 199.00, 100);
INSERT INTO `product_sku` VALUES (79, 35, '降噪版 白色', 259.00, 60);
INSERT INTO `product_sku` VALUES (80, 36, '黑色', 89.00, 120);
INSERT INTO `product_sku` VALUES (81, 36, '白色', 89.00, 100);
INSERT INTO `product_sku` VALUES (82, 37, '灰色', 149.00, 80);
INSERT INTO `product_sku` VALUES (83, 37, '银色', 149.00, 70);
INSERT INTO `product_sku` VALUES (84, 38, '黑色 青轴', 299.00, 50);
INSERT INTO `product_sku` VALUES (85, 38, '白色 红轴', 329.00, 40);
INSERT INTO `product_sku` VALUES (86, 39, '黑色', 79.00, 100);
INSERT INTO `product_sku` VALUES (87, 39, '白色', 79.00, 80);
INSERT INTO `product_sku` VALUES (88, 40, '标准款 1.6m', 25.00, 150);
INSERT INTO `product_sku` VALUES (89, 40, '加强款 2.0m', 35.00, 100);
INSERT INTO `product_sku` VALUES (90, 41, '1.2m 黑色', 35.00, 200);
INSERT INTO `product_sku` VALUES (91, 41, '1.8m 白色', 45.00, 150);
INSERT INTO `product_sku` VALUES (92, 42, '静音款 白色', 59.00, 118);
INSERT INTO `product_sku` VALUES (93, 42, '摇头款 蓝色', 79.00, 62);

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色编码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 99 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, '管理员', 'ADMIN');
INSERT INTO `role` VALUES (2, '商家', 'MERCHANT');
INSERT INTO `role` VALUES (3, '普通用户', 'CUSTOMER');
INSERT INTO `role` VALUES (99, '超级管理员', 'SUPER_ADMIN');

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_rp`(`role_id`, `permission_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permission
-- ----------------------------
INSERT INTO `role_permission` VALUES (1, 1, 1);
INSERT INTO `role_permission` VALUES (2, 1, 2);
INSERT INTO `role_permission` VALUES (3, 1, 3);
INSERT INTO `role_permission` VALUES (4, 1, 4);
INSERT INTO `role_permission` VALUES (5, 1, 5);
INSERT INTO `role_permission` VALUES (6, 1, 6);
INSERT INTO `role_permission` VALUES (7, 2, 2);
INSERT INTO `role_permission` VALUES (8, 2, 4);
INSERT INTO `role_permission` VALUES (9, 3, 3);
INSERT INTO `role_permission` VALUES (10, 3, 6);

-- ----------------------------
-- Table structure for shop
-- ----------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '店铺名称',
  `shop_logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺LOGO',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺简介',
  `status` tinyint NULL DEFAULT 1 COMMENT '0关闭 1营业',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_merchant`(`merchant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shop
-- ----------------------------
INSERT INTO `shop` VALUES (1, 1, '数码官方旗舰店', 'https://picsum.photos/200/200', '专注手机数码产品', 1, '2026-06-07 13:54:04');
INSERT INTO `shop` VALUES (2, 2, '优品生活百货', 'https://picsum.photos/200/200', '各类生活用品一站式购齐', 1, '2026-06-07 13:54:04');
INSERT INTO `shop` VALUES (3, 3, '美妆时尚旗舰店', 'https://picsum.photos/200/200?random=100', '美妆护肤、时尚穿搭一站式购物', 1, '2026-06-14 00:00:00');
INSERT INTO `shop` VALUES (4, 4, '王五数码精品店', 'https://picsum.photos/200/200?random=200', '正品数码配件，品质保证，7天无理由退换', 1, '2026-01-01 00:00:00');
INSERT INTO `shop` VALUES (6, 5, '手机专卖', NULL, '手机专卖，欢迎光临！', 1, '2026-06-21 00:00:28');
INSERT INTO `shop` VALUES (9, 6, '11', NULL, '11，欢迎光临！', 1, '2026-06-21 00:14:33');
INSERT INTO `shop` VALUES (19, 7, '大大', NULL, '大大，欢迎光临！', 1, '2026-06-26 21:49:05');

-- ----------------------------
-- Table structure for shop_follow
-- ----------------------------
DROP TABLE IF EXISTS `shop_follow`;
CREATE TABLE `shop_follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_shop`(`user_id`, `shop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺关注' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shop_follow
-- ----------------------------
INSERT INTO `shop_follow` VALUES (1, 1, 1, '2026-06-10 10:00:00');
INSERT INTO `shop_follow` VALUES (2, 1, 2, '2026-06-11 11:00:00');
INSERT INTO `shop_follow` VALUES (3, 2, 2, '2026-06-12 12:00:00');
INSERT INTO `shop_follow` VALUES (4, 3, 1, '2026-06-13 13:00:00');
INSERT INTO `shop_follow` VALUES (8, 1, 4, '2026-06-11 10:00:00');
INSERT INTO `shop_follow` VALUES (9, 2, 4, '2026-06-12 11:00:00');
INSERT INTO `shop_follow` VALUES (10, 8, 4, '2026-06-13 12:00:00');

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作账号',
  `oper_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：登录/商品/订单/权限/商家',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作详情',
  `ip` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `status` tinyint NULL DEFAULT 1 COMMENT '1成功，0失败',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_log
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `gender` tinyint NULL DEFAULT 0 COMMENT '0保密 1男 2女',
  `signature` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个性签名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密密码',
  `role_id` bigint NOT NULL COMMENT '关联角色ID',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0禁用1正常',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username`) USING BTREE,
  UNIQUE INDEX `idx_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '前台用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'zhangsan', 'zhangsan', 0, NULL, '$2a$10$wXE4VqqdiUbwtHObTfHpPemdXTW4fhug3f5ElhumhTE4QWIgqHKDS', 1, '13800138001', 'zhangsan@qq.com', 'https://picsum.photos/200/200', 1, '2026-06-07 13:54:04', '2026-06-20 16:01:15');
INSERT INTO `user` VALUES (2, 'lisi', 'lisi', 0, NULL, '$2a$10$UrnkUQ/i2ReXs3jHO1t2DO1zVWwqluaoj/CUL3ffZs9ehlr1WncFe', 3, '13800138002', 'lisi@qq.com', 'https://picsum.photos/200/200', 1, '2026-06-07 13:54:04', '2026-06-20 16:02:01');
INSERT INTO `user` VALUES (3, 'wangwu', '王五', 1, '数码爱好者，诚信经营', '$2a$10$3oSbwrGdG0RdY1fFUpT1HevPZIU.PVxCjNS3KKCBBat3HOEWYYwKe', 2, '13800138003', 'wangwu@qq.com', '/api/upload/avatar/579dfeb2-2a3a-44a2-8d4a-92623b6e8a93.jpg', 1, '2026-01-01 00:00:00', '2026-06-20 21:56:30');
INSERT INTO `user` VALUES (4, 'zhaoliu', 'zhaoliu', 0, NULL, '$2a$10$a3eYoMebQYs/wfeVzXzlEusskRsD6EO4wjONn97qA/4qi.FI8k2Bu', 3, '13800138004', 'zhaoliu@qq.com', 'https://picsum.photos/200/200', 1, '2026-06-07 13:54:04', '2026-06-20 16:03:36');
INSERT INTO `user` VALUES (5, 'sunqi', 'sunqi', 0, '', '$2a$10$JMEUtzaC/NbrLEu6dGPgiezLxTMQfq0ZBf5tYG1EoWefZ/iRyiz3q', 3, '13800138005', 'sunqi@qq.com', '/api/upload/avatar/947c23a5-78e7-4308-93d1-73d9d6828b9d.jpg', 1, '2026-06-07 13:54:04', '2026-06-22 23:53:22');
INSERT INTO `user` VALUES (6, 'shopuser', 'shopuser', 0, NULL, '$2a$10$2jVJ6JgknVnm8TX00/k9HuQVb5BdNwDLvMwb9GQENycBjaoRhjcfa', 99, '13800138006', 'shop@qq.com', 'https://picsum.photos/200/200', 1, '2026-06-07 13:54:04', '2026-06-20 15:49:25');
INSERT INTO `user` VALUES (7, '用户8007', '用户8007', 0, NULL, '$2a$10$HMTqU75JP04DpwQ9feIiwOOXQNvx4kFMApy4/RNeoC47YihKj3/gu', 1, '13800138007', NULL, NULL, 1, '2026-06-09 23:08:19', '2026-06-20 16:04:12');
INSERT INTO `user` VALUES (8, 'beautyshop', '美妆店主', 1, '专注美妆时尚', '$2a$10$EZ7BnJ9Qo', 3, '13800138008', 'beauty@qq.com', 'https://picsum.photos/200/200', 1, '2026-06-14 00:00:00', '2026-06-14 13:16:14');
INSERT INTO `user` VALUES (9, 'testuser', 'testuser', 0, NULL, '123456', 2, '13800138000', NULL, NULL, 1, '2026-06-17 20:14:06', '2026-06-19 18:48:38');
INSERT INTO `user` VALUES (10, 'paytest', 'paytest', 0, NULL, '123456', 2, '13900139000', NULL, NULL, 1, '2026-06-17 20:15:50', '2026-06-19 18:48:36');
INSERT INTO `user` VALUES (11, '1687847063', NULL, 0, NULL, '$2a$10$J22cEhHwepdXvkISal3cy.Ktk3buv95Z8AzlfBYx67gMqLJqDtSZS', 3, NULL, '1687847063@qq.com', NULL, 1, '2026-06-20 23:38:55', '2026-06-21 00:04:40');

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0未用1已用2过期',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户领取优惠券' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_coupon
-- ----------------------------
INSERT INTO `user_coupon` VALUES (1, 1, 1, 0, '2026-06-07 13:54:04');
INSERT INTO `user_coupon` VALUES (2, 1, 2, 0, '2026-06-07 13:54:04');
INSERT INTO `user_coupon` VALUES (3, 2, 1, 0, '2026-06-07 13:54:04');
INSERT INTO `user_coupon` VALUES (4, 3, 3, 0, '2026-06-07 13:54:04');
INSERT INTO `user_coupon` VALUES (5, 4, 1, 0, '2026-06-07 13:54:04');
INSERT INTO `user_coupon` VALUES (6, 3, 1, 1, '2026-06-17 23:21:38');
INSERT INTO `user_coupon` VALUES (7, 3, 2, 0, '2026-06-17 23:22:42');
INSERT INTO `user_coupon` VALUES (8, 3, 2, 0, '2026-06-19 03:32:02');
INSERT INTO `user_coupon` VALUES (9, 3, 1, 1, '2026-06-19 03:32:03');
INSERT INTO `user_coupon` VALUES (10, 5, 2, 0, '2026-06-20 16:07:23');
INSERT INTO `user_coupon` VALUES (11, 5, 1, 0, '2026-06-20 16:07:32');
INSERT INTO `user_coupon` VALUES (12, 12, 2, 0, '2026-07-09 20:52:12');
INSERT INTO `user_coupon` VALUES (13, 12, 1, 0, '2026-07-09 20:52:12');

SET FOREIGN_KEY_CHECKS = 1;
