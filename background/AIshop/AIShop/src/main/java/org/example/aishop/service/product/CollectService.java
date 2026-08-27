package org.example.aishop.service.product;

import java.util.List;

public interface CollectService {
    void addCollect(Long productId);
    void removeCollect(Long productId);
    boolean isCollected(Long productId);
    List<Long> listCollectedProductIds();
    int getCollectCount(Long productId);
}