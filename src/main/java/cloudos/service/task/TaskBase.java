package cloudos.service.task;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.MultiViolationException;

import java.util.List;
import java.util.concurrent.Callable;

@Accessors(chain=true)
public abstract class TaskBase implements Callable<TaskResult> {

    @Getter @Setter protected TaskId taskId;
    @Getter @Setter protected TaskResult result = new TaskResult();

    public void init() {
        taskId = new TaskId();
        taskId.initUUID();
    }

    protected void description(String messageKey, String target) { result.description(messageKey, target); }

    protected void addEvent(String messageKey) { result.add(new TaskEvent(this, messageKey)); }

    protected void error(String messageKey, Exception e) { result.error(new TaskEvent(this, messageKey), e); }

    protected void error(String messageKey, String message) { error(messageKey, new Exception(message)); }

    protected void error(String messageKey, List<ConstraintViolationBean> errors) {
        error(messageKey, new MultiViolationException(errors));
    }

}
