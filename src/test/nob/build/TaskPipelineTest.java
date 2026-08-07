package nob.build;

import nob.Task;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class TaskPipelineTest {

    @Test
    public void seeResult() {
        TaskPipeline pl = new TaskPipeline();
        pl.reg(NamespaceId.parse("HI:t"), Tt.class);
        pl.to();
    }
}

class Tt implements Task {
    public String id() {
        return "HI";
    }

    public List<NamespaceId> produces() {
        return namespaceIdsFrom("hi:hello");
    }

    public void execute(Context ctx) {
    }
}
