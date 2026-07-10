package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.PortfolioAsset;
import com.smartoa.mapper.PortfolioAssetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService 资产价格更新测试（id 路由，非 symbol）")
class PortfolioServiceAssetPriceTest {

    @Mock
    private PortfolioAssetMapper assetMapper;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    @DisplayName("assetId=42 时调用 assetMapper.selectById(42L)")
    void updateAssetPrice_selectsById() {
        PortfolioAsset asset = new PortfolioAsset();
        asset.setId(42L);
        asset.setSymbol("AAPL");
        when(assetMapper.selectById(42L)).thenReturn(asset);
        when(assetMapper.updateById(any(PortfolioAsset.class))).thenReturn(1);

        portfolioService.updateAssetPrice(42L, new BigDecimal("100.1234"));

        verify(assetMapper).selectById(42L);
        verify(assetMapper).updateById(asset);
        assertEquals(new BigDecimal("100.1234"), asset.getCurrentPrice());
        assertEquals(LocalDate.now(), asset.getPriceDate());
    }

    @Test
    @DisplayName("不再进行 symbol 查询")
    void updateAssetPrice_noSymbolQuery() {
        PortfolioAsset asset = new PortfolioAsset();
        asset.setId(1L);
        when(assetMapper.selectById(1L)).thenReturn(asset);
        when(assetMapper.updateById(any(PortfolioAsset.class))).thenReturn(1);

        portfolioService.updateAssetPrice(1L, new BigDecimal("50.0000"));

        verify(assetMapper).selectById(1L);
        verify(assetMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("资产不存在抛业务异常")
    void updateAssetPrice_notFound_throwsBusinessException() {
        when(assetMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portfolioService.updateAssetPrice(999L, new BigDecimal("100.0000")));
        assertTrue(ex.getMessage().contains("资产标的不存在"));
        verify(assetMapper, never()).updateById(any(PortfolioAsset.class));
    }

    @Test
    @DisplayName("合法价格按4位小数保存")
    void updateAssetPrice_scalesToFourDecimals() {
        PortfolioAsset asset = new PortfolioAsset();
        asset.setId(1L);
        when(assetMapper.selectById(1L)).thenReturn(asset);
        when(assetMapper.updateById(any(PortfolioAsset.class))).thenReturn(1);

        portfolioService.updateAssetPrice(1L, new BigDecimal("100.1"));

        assertEquals(new BigDecimal("100.1000"), asset.getCurrentPrice());
    }
}
