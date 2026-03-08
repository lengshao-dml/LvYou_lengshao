package com.textoasis.startup;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.textoasis.model.City;
import com.textoasis.model.CityFeature;
import com.textoasis.model.Tag;
import com.textoasis.repository.CityRepository;
import com.textoasis.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CityRepository cityRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 如果数据库中已有城市数据，则不执行导入
        if (cityRepository.count() > 0) {
            return;
        }

        loadCityData();
    }

    private void loadCityData() throws IOException, CsvValidationException {
        // 将标签标题和预先创建的Tag实体映射起来，方便快速查找
        Map<String, Tag> tagCache = new HashMap<>();

        // 预期CSV文件中的标题行 (16列)
        String[] headers = {"city","自然风光","历史文化","主题乐园","城市观光","美食文化","海滨休闲","休闲康养","户外运动","宗教信仰","节庆民俗","province","pinyin","abbr","latitude","longitude"};
        for (int i = 1; i <= 10; i++) { // 循环创建“自然风光”到“节庆民俗”等标签
            final String tagName = headers[i];
            Tag tag = tagRepository.findByName(tagName).orElseGet(() -> {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                newTag.setCategory("景点类型");
                return tagRepository.save(newTag);
            });
            tagCache.put(tagName, tag);
        }

        // 从classpath下的data/cities.csv读取文件
        ClassPathResource resource = new ClassPathResource("data/cities.csv");
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            String[] line;
            reader.readNext(); // 跳过标题行

            while ((line = reader.readNext()) != null) {
                // 如果城市名称为空，则视为无效行并跳过
                if (line.length < 16 || line[0] == null || line[0].trim().isEmpty()) {
                    continue;
                }

                // 检查经纬度是否存在
                if (line[14] == null || line[14].trim().isEmpty() || line[15] == null || line[15].trim().isEmpty()) {
                    System.out.println("WARN: Skipping city due to missing geo-data: " + line[0]);
                    continue;
                }

                // 创建并保存City实体
                City city = new City();
                city.setName(line[0]);
                city.setProvince(line[11]);
                city.setPinyin(line[12]);
                city.setAbbr(line[13]);
                city.setLatitude(new BigDecimal(line[14]));
                city.setLongitude(new BigDecimal(line[15]));
                city.setFeatures(new HashSet<>());
                city.setHotnessScore(0);

                // 为每个标签下的描述创建CityFeature
                for (int i = 1; i <= 10; i++) {
                    String description = line[i];
                    if (description != null && !description.trim().isEmpty()) {
                        Tag tag = tagCache.get(headers[i]);
                        CityFeature feature = new CityFeature();
                        feature.setCity(city);
                        feature.setTag(tag);
                        feature.setDescription(description);
                        city.getFeatures().add(feature);
                    }
                }
                cityRepository.save(city);
            }
        }
    }
}
