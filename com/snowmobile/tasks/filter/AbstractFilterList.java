package com.snowmobile.tasks.filter;

import com.snowmobile.tasks.TaskItem;
import java.util.List;

public abstract class AbstractFilterList {
    public abstract List<TaskItem> filter(List<TaskItem> list);
}
