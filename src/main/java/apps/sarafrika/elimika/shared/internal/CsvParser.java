package apps.sarafrika.elimika.shared.internal;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvParser {

    public List<TemplateData> processCsv(MultipartFile file) throws IOException {
        List<TemplateData> emailDataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = null;
            String line;
            int rowIndex = 0;

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");

                if (rowIndex == 0) {
                    // First row contains headers
                    headers = values;
                } else {
                    // Validate structure
                    if (values.length != headers.length) {
                        throw new IllegalArgumentException("Invalid CSV structure");
                    }

                    String email = values[0];
                    Map<String, String> placeholders = new HashMap<>();
                    for (int i = 0; i < values.length; i++) {
                        placeholders.put(headers[i], values[i]);
                    }

                    emailDataList.add(new TemplateData(email, placeholders));
                }
                rowIndex++;
            }
        }

        return emailDataList;
    }
}

