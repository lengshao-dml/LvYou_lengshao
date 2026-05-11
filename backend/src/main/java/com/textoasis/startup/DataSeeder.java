package com.textoasis.startup;

import com.textoasis.model.*;
import com.textoasis.repository.CityPopularityRepository;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final CityPopularityRepository popularityRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 如果数据库中已有城市数据，则不执行导入
        if (cityRepository.count() > 0) {
            return;
        }

        loadCityData();
    }

    /**
     * 手动解析CSV行，正确处理字段内包含逗号的情况。
     * CSV标准：双引号包裹的字段中的逗号不作为分隔符，连续双引号转义。
     * 兼容中文引号「」的情况。
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inChineseQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            char next = i + 1 < line.length() ? line.charAt(i + 1) : 0;

            // 处理英文双引号转义
            if (c == '"' && !inChineseQuotes) {
                if (inQuotes && next == '"') {
                    // 转义的引号
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            // 处理中文引号（兼容「」作为引号对）
            if (c == '「') {
                inChineseQuotes = true;
                current.append(c);
                continue;
            }
            if (c == '」') {
                inChineseQuotes = false;
                current.append(c);
                continue;
            }

            // 逗号分隔：仅在不在引号内时
            if (c == ',' && !inQuotes && !inChineseQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private void loadCityData() throws IOException {
        // 将标签标题和预先创建的Tag实体映射起来，方便快速查找
        Map<String, Tag> tagCache = new HashMap<>();

        // 预期CSV文件中的标题行 (16列)
        String[] headers = {"city", "自然风光", "历史文化", "主题乐园", "城市观光", "美食文化", "海滨休闲",
                "休闲康养", "户外运动", "宗教信仰", "节庆民俗", "province", "pinyin", "abbr", "latitude", "longitude"};
        for (int i = 1; i <= 10; i++) {
            final String tagName = headers[i];
            Tag tag = tagRepository.findByName(tagName).orElseGet(() -> {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                newTag.setCategory("景点类型");
                return tagRepository.save(newTag);
            });
            tagCache.put(tagName, tag);
        }

        // 从classpath下的data/cities.csv读取文件，逐行手动解析
        ClassPathResource resource = new ClassPathResource("data/cities.csv");
        List<City> cities = new ArrayList<>();
        int totalCities = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String rawLine;
            reader.readLine(); // 跳过标题行

            while ((rawLine = reader.readLine()) != null) {
                if (rawLine.trim().isEmpty()) {
                    continue;
                }

                // 手动解析CSV行，正确处理字段内的逗号和引号
                List<String> fields = parseCsvLine(rawLine);

                // 检查字段数是否足够
                if (fields.size() < 16) {
                    System.out.println("WARN: Skipping city, expected 16 fields but got "
                            + fields.size() + ": " + fields.get(0));
                    skipped++;
                    continue;
                }

                // 检查经纬度是否存在
                String latStr = fields.get(14).trim();
                String lngStr = fields.get(15).trim();
                if (latStr.isEmpty() || lngStr.isEmpty()) {
                    System.out.println("WARN: Skipping city due to missing geo-data: " + fields.get(0));
                    skipped++;
                    continue;
                }

                // 创建City实体
                City city = new City();
                city.setName(fields.get(0));
                city.setProvince(fields.get(11));
                city.setPinyin(fields.get(12));
                city.setAbbr(fields.get(13));
                city.setLatitude(new BigDecimal(latStr));
                city.setLongitude(new BigDecimal(lngStr));
                city.setFeatures(new HashSet<>());
                city.setHotnessScore(0);
                int totalAttractions = 0;

                // 为每个标签下的描述创建CityFeature和Attractions
                for (int i = 1; i <= 10; i++) {
                    String attractionsString = fields.get(i);
                    if (attractionsString == null || attractionsString.trim().isEmpty()) {
                        continue;
                    }
                    Tag tag = tagCache.get(headers[i]);
                    if (tag == null) {
                        continue;
                    }

                    CityFeature feature = new CityFeature();
                    feature.setCity(city);
                    feature.setTag(tag);
                    feature.setAttractions(new HashSet<>());

                    // 按斜杠分割多个景点
                    String[] attractionParts = attractionsString.split("/");
                    for (String part : attractionParts) {
                        part = part.trim();
                        if (part.isEmpty()) continue;

                        // 使用正则表达式解析景点名称和描述：名称（描述）
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([^（]+)（([^）]+)）");
                        java.util.regex.Matcher matcher = pattern.matcher(part);
                        if (matcher.find()) {
                            Attraction attraction = new Attraction();
                            attraction.setName(matcher.group(1).trim());
                            attraction.setDescription(matcher.group(2).trim());
                            attraction.setCityFeature(feature);
                            feature.getAttractions().add(attraction);
                            totalAttractions++;
                        } else {
                            // fallback：没有括号描述时，把整段文本作为景点名
                            Attraction attraction = new Attraction();
                            attraction.setName(part);
                            attraction.setDescription("");
                            attraction.setCityFeature(feature);
                            feature.getAttractions().add(attraction);
                            totalAttractions++;
                        }
                    }
                    city.getFeatures().add(feature);
                }

                // 以景点总数作为初始热度分
                city.setHotnessScore(totalAttractions);
                cities.add(city);
                totalCities++;

                // 每100条批量flush一次，避免内存积压
                if (cities.size() % 100 == 0) {
                    cityRepository.saveAll(cities);
                    cityRepository.flush();
                    cities.clear();
                }
            }

            // 保存剩余的城市
            if (!cities.isEmpty()) {
                cityRepository.saveAll(cities);
            }
        }

        System.out.println("DataSeeder: Imported " + totalCities + " cities, skipped " + skipped + ".");

        // 初始化城市热度数据（基于景点数作为初始热度）
        initCityPopularity();
    }

    /**
     * 为所有城市初始化热力数据，以景点总数作为初始热度分数
     */
    @Transactional
    protected void initCityPopularity() {
        // 检查是否已有数据
        if (popularityRepository.count() > 0) {
            return;
        }

        List<City> allCities = cityRepository.findAll();
        if (allCities.isEmpty()) {
            return;
        }

        // 计算所有城市的景点数
        long maxAttractions = allCities.stream()
                .mapToLong(c -> c.getFeatures() != null
                        ? c.getFeatures().stream().mapToLong(f -> f.getAttractions() != null
                        ? f.getAttractions().size() : 0).sum()
                        : 0)
                .max().orElse(1);

        if (maxAttractions == 0) maxAttractions = 1;

        List<CityPopularity> popularities = new ArrayList<>();
        for (City city : allCities) {
            long totalAttractions = city.getFeatures() != null
                    ? city.getFeatures().stream().mapToLong(f -> f.getAttractions() != null
                    ? f.getAttractions().size() : 0).sum()
                    : 0;

            double baseScore = (double) totalAttractions / maxAttractions * 100.0;

            CityPopularity cp = new CityPopularity();
            cp.setCity(city);
            cp.setClickCount(0L);
            cp.setSearchCount(0L);
            cp.setBaseScore(baseScore);
            cp.setScore(baseScore);
            popularities.add(cp);
        }

        popularityRepository.saveAll(popularities);
        System.out.println("DataSeeder: Initialized popularity data for " + popularities.size() + " cities.");
    }
}
