package com.yeyamo_mobile.api.ingestion_service.application.source;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
@Component
public class CsvSourceStrategy implements IngestionSourceStrategy {
    private final PayloadLimits limits;
    public CsvSourceStrategy(){this(PayloadLimits.defaults());}
    @Autowired public CsvSourceStrategy(PayloadLimits limits){this.limits=limits;}
    public boolean supports(SourceType t){return t==SourceType.CSV;}
    public List<RawRecord> extract(ImportJob job){
        List<List<String>> rows=parse(job.getInputPayload());
        if(rows.isEmpty())return List.of();
        if(rows.size()-1>limits.maximumRecords())throw new IllegalArgumentException("CSV contains too many records");
        if(rows.getFirst().size()>limits.maximumFields())throw new IllegalArgumentException("CSV contains too many columns");
        List<String> headers=rows.getFirst().stream().map(this::key).toList();
        if(headers.stream().anyMatch(String::isBlank)||new HashSet<>(headers).size()!=headers.size())
            throw new IllegalArgumentException("CSV headers must be non-empty and unique");
        List<RawRecord> result=new ArrayList<>();
        for(int i=1;i<rows.size();i++){
            if(rows.get(i).stream().allMatch(String::isBlank))continue;
            Map<String,String> values=new LinkedHashMap<>();
            if(rows.get(i).size()>limits.maximumFields())throw new IllegalArgumentException("CSV row contains too many columns");
            for(int c=0;c<headers.size();c++){String cell=c<rows.get(i).size()?rows.get(i).get(c).trim():"";if(cell.length()>limits.maximumValueLength())throw new IllegalArgumentException("CSV field value is too long");values.put(headers.get(c),cell);}
            result.add(new RawRecord(i+1,values));
        }
        return result;
    }
    private List<List<String>> parse(String csv){
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;
        for(int i=0;i<csv.length();i++){char ch=csv.charAt(i);
            if(ch=='"'){if(quoted&&i+1<csv.length()&&csv.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}
            else if(ch==','&&!quoted){row.add(value.toString());value.setLength(0);}
            else if((ch=='\n'||ch=='\r')&&!quoted){if(ch=='\r'&&i+1<csv.length()&&csv.charAt(i+1)=='\n')i++;row.add(value.toString());value.setLength(0);rows.add(row);row=new ArrayList<>();}
            else value.append(ch);
        }
        if(quoted)throw new IllegalArgumentException("Malformed CSV quoted value");
        if(!row.isEmpty()||value.length()>0){row.add(value.toString());rows.add(row);}return rows;
    }
    private String key(String v){return v==null?"":v.trim().toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_');}
}
