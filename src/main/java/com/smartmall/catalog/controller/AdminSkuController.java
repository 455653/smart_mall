package com.smartmall.catalog.controller;

import com.smartmall.catalog.dto.StockAdjustRequest;
import com.smartmall.catalog.service.SkuStockService;
import com.smartmall.catalog.vo.SkuVO;
import com.smartmall.common.api.Result;
import com.smartmall.common.security.RequireRole;
import com.smartmall.common.security.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台库存管理接口层。
 *
 * <p><b>接口清单</b>：
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>PUT</td><td>/api/admin/skus/{id}/stock</td><td>增量调整库存（正入库、负出库）</td></tr>
 * </table>
 *
 * <p><b>为什么用增量（{@code delta}）而不是「设置成 N」</b>：
 * 两个运营同时操作同一个 SKU 时，「设为 100」会让后写的覆盖先写的；
 * 而「加 10 件」「减 5 件」能正确叠加。绝对值接口在并发下必然丢更新。
 *
 * <p><b>最关键的验收点</b>：{@code delta < 0} 复用条件更新（{@code where stock >= ?}），
 * 因此「把库存扣穿」必然返回 12003，<b>且库存数字一个都不动</b>。
 */
@RestController                                  // REST 控制器
@RequestMapping("/api/admin/skus")               // 公共路径前缀：后台 + 规格
@RequireRole({Role.OPERATOR, Role.ADMIN})        // 类级鉴权：运营或管理员，满足其一即可
@RequiredArgsConstructor                         // 构造器注入
public class AdminSkuController {

    // 库存服务：面向接口注入，将来换成 Redis 预扣实现，本类不用改
    private final SkuStockService skuStockService;

    /**
     * 调整库存。
     *
     * <p>返回值里的 {@code stock} 是<b>调整后重新查出来的真实库存</b>，
     * 不是「原库存 + delta」算出来的，因为条件更新不回写实体。
     *
     * @param id      规格 ID（从 URL 路径取）
     * @param request 只含 delta 的请求体：正数入库、负数出库、0 表示不调整
     * @return 调整后的 SKU（含最新库存）
     */
    @PutMapping("/{id}/stock")
    public Result<SkuVO> adjustStock(@PathVariable Long id,
                                     @Valid @RequestBody StockAdjustRequest request) {
        // 调服务：正数走回补、负数走扣减（扣减是条件更新，不够就抛 12003）
        var sku = skuStockService.adjust(id, request.delta());
        return Result.ok(SkuVO.of(sku));
    }
}
