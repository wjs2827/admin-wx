package com.ymsz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ymsz.config.RedisCacheManager;
import com.ymsz.pojo.Combo;
import com.ymsz.pojo.ComboDish;
import com.ymsz.pojo.Dish;
import com.ymsz.service.BaseService;
import com.ymsz.service.DishService;
import com.ymsz.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Happysnaker
 * @description
 * @date 2021/10/22
 * @email happysnaker@foxmail.com
 */
@Service
public class DishServiceImpl extends BaseService implements DishService {
    private List<Combo> getCombos() {
        List<Combo> combos = comboMapper.queryComboInfo();

        //为 combo 注入图片、折扣等信息
        for (Combo combo : combos) {
            for (ComboDish dish : combo.getComboDish()) {
                if (combo.getImgs() == null) {
                    combo.setImgs(new ArrayList<>());
                }
                combo.getImgs().add(String.valueOf(dish.getDishImg()));
            }
            combo.setShopDiscount(combo.getDiscount() != null ? combo.getDiscount().getDiscount(combo.getPrice()) : 0);
        }
        return combos;
    }

    /**
     * 同步后端数据，例如上架状态、是否推荐等，这类数据以 Redis 中为准，例如，如果后端显示下架，那么将不会返回给前端，由于后端也是采用 Redis 做缓存，未刷新到数据库中，因此不能从数据库中取
     *
     * @param dishes    菜品
     * @param pubStatus 是否需要同步上架状态
     * @param recStatus 是否需要同步推荐状态
     * @param newStatus 是否需要同步新品状态
     * @return
     */
    public List<Dish> synchronizeDishBackendStatus(List<Dish> dishes, boolean pubStatus, boolean recStatus, boolean newStatus) {
        return dishes.stream().filter((dish -> {
            // 要么未设置 pubStatus，否则进行判断，如果 redis 无 key，默认通过，否则对比缓存数据
            boolean b1 = (!pubStatus ||
                    (!redisManager.hasKey(RedisCacheManager.DISH_PUBLISH_STATUS_KEY)
                            || redisManager.getBit(RedisCacheManager.DISH_PUBLISH_STATUS_KEY, (long) dish.getId())));
            boolean b2 = (!recStatus ||
                    (!redisManager.hasKey(RedisCacheManager.DISH_RECOMMEND_STATUS_KEY)
                            || redisManager.getBit(RedisCacheManager.DISH_RECOMMEND_STATUS_KEY, (long) dish.getId())));
            boolean b3 = (!newStatus ||
                    (!redisManager.hasKey(RedisCacheManager.DISH_NEW_STATUS_KEY)
                            || redisManager.getBit(RedisCacheManager.DISH_NEW_STATUS_KEY, (long) dish.getId())));
            return b1 && b2 && b3;
        })).collect(Collectors.toList());
    }

    /**
     * 同步套餐上架状态
     *
     * @param combos
     * @return
     */
    public List<Combo> synchronizeComboBackendStatus(List<Combo> combos) {
        return combos.stream().filter((combo -> {
            // 套餐中的单点菜品如果下架，则套餐不显示
            for (ComboDish comboDish : combo.getComboDish()) {
                boolean b = (!redisManager.hasKey(RedisCacheManager.DISH_PUBLISH_STATUS_KEY) || redisManager.getBit(RedisCacheManager.DISH_PUBLISH_STATUS_KEY, (long) comboDish.getDishId()));
                if (b == false) {
                    return false;
                }
            }
            // 要么未设置 pubStatus，否则进行判断，如果 redis 无 key，默认通过，否则对比缓存数据
            return (!redisManager.hasKey(RedisCacheManager.DISH_PUBLISH_STATUS_KEY)
                    || redisManager.getBit(RedisCacheManager.COMBO_PUBLISH_STATUS_KEY, (long) combo.getId()));
        })).collect(Collectors.toList());
    }

    @Override
    public String getIndexDishInfo() {
        //如果有，直接从缓存中拿
        if (redisManager.hasKey(RedisCacheManager.INDEX_DISH_INFO_CACHE_KEY)) {
            return (String) redisManager.getForValue(RedisCacheManager.INDEX_DISH_INFO_CACHE_KEY);
        }
        // 上架状态等从 Redis 中拿，与后台同步
        List<Dish> hotSaleDish = synchronizeDishBackendStatus(
                dishMapper.queryHotSaleDish(15),
                true, false, false
        );

        List<Dish> newDish = synchronizeDishBackendStatus(
                dishMapper.queryNewDish(),
                true, false, true
        );

        List<Dish> recommendedDish = synchronizeDishBackendStatus(
                dishMapper.queryRecommendedDish(),
                true, true, false
        );


        List<Combo> combos = synchronizeComboBackendStatus(getCombos());

        JSONObject object = new JSONObject();
        JsonUtils.listAddToJsonObject(object, hotSaleDish, "hotDishList");
        JsonUtils.listAddToJsonObject(object, newDish, "newDishList");
        JsonUtils.listAddToJsonObject(object, recommendedDish, "recommendDishList");
        JsonUtils.listAddToJsonObject(object, combos, "combos");

        // 添加缓存，由于菜品信息数据静态数据，缓存时间可以长一点
        // 但是后台管理员可能会变更菜品信息，太久了也是不合适的，因此设置为 3 分钟
        redisManager.addIfAbsentForValue(RedisCacheManager.INDEX_DISH_INFO_CACHE_KEY, object.toJSONString(), 60 * 3, TimeUnit.SECONDS);
        return object.toJSONString();
    }

    @Override
    public String getOrderDishInfo(int storeId) {
        //这种点餐的数据建议不要从缓存中取
        List<Dish> dishes = synchronizeDishBackendStatus(dishMapper.queryDishInfo(storeId), true, false, false);
        List<Combo> combos = synchronizeComboBackendStatus(getCombos());
        JSONObject object = new JSONObject();
        JsonUtils.listAddToJsonObject(object, dishes, "dishes");
        JsonUtils.listAddToJsonObject(object, combos, "combos");
        return object.toJSONString();
    }

    @Override
    public List<Dish> getDishInfo(int storeId) {
        List<Dish> dishes = dishMapper.queryDishInfo(storeId);
        JSONObject object = new JSONObject();
        return dishes;
    }

    @Override
    public List<Map<String, String>> getDishClassification() {
        List<Map<String, String>> list = dishMapper.queryClassification();
        return list;
    }

    @Override
    public List<Dish> getUserCollectedDishes(String userId) {
        Set<Integer> set = new HashSet<>(userMapper.queryCollectedDish(userId));
        List<Dish> dishes = dishMapper.queryDishes();
        List<Combo> combos = comboMapper.queryComboInfo();

        List<Dish> ans = new LinkedList<>();
        try {
            for (Dish dish : dishes) {
                //从缓存中查询，维护数据一致性
                if (redisManager.getBit(RedisCacheManager.getUserCollectedDishCacheKey(userId), (long) dish.getId())) {
                    ans.add(dish);
                }
            }

            //将 combo 抽象成 dish 一起返回
            for (Combo combo : combos) {
                if (redisManager.getBit(RedisCacheManager.getUserCollectedDishCacheKey(userId), (long) combo.getId())) {
                    Dish dish = new Dish();
                    dish.setId(combo.getId());
                    //收藏页面无法容纳多张照片，因此注入第一个菜品的图片
                    dish.setDishImg((String) combo.getComboDish().get(0).getDishImg());
                    dish.setPrice(combo.getPrice());
                    dish.setSale(combo.getSale());
                    dish.setName(combo.getName());
                    dish.setTags(combo.getTags());
                    dish.setId(combo.getId());
                    ans.add(dish);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //可能是 redis 中缓存过期，初始化用户收藏的菜品和套餐，并重试
            redisManager.initRedisUserMarkedCache(
                    dishMapper.queryAllDishId(),
                    userMapper.queryCollectedDish(userId),
                    RedisCacheManager.getUserCollectedDishCacheKey(userId));
            return getUserCollectedDishes(userId);
        }
        return ans;
    }

    @Override
    public List<Map<String, Integer>> getDishLikeNumDeltaList() {
        List<Map<String, Integer>> ans = new ArrayList<>();
        for (Integer id : dishMapper.queryAllDishId()) {
            Map<String, Integer> map = new HashMap<>();
            map.put("dishId", id);
            try {
                if (id == 1) {
                    System.out.println(1);
                }
                int likeNum = (int) redisManager.getForHash(RedisCacheManager.DISH_LIKE_NUM_CACHE_KEY, id);
                Object o = redis.opsForHash().get(RedisCacheManager.DISH_LIKE_NUM_CACHE_KEY, id);
                map.put("likeNumDelta", likeNum);
                ans.add(map);
            } catch (Exception e) {
                // 缓存中可能为空，如果为空说明数据未变化，那么什么也不做
            }
        }
        for (Map<String, Integer> map : ans) {
            System.out.println("map = " + map);
        }
        return ans;
    }
}
