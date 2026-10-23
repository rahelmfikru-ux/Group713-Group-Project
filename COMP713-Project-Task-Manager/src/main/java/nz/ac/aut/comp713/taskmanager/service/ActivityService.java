package nz.ac.aut.comp713.taskmanager.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import nz.ac.aut.comp713.taskmanager.model.ActivityEntry;
import nz.ac.aut.comp713.taskmanager.repository.ActivityRepository;

import java.util.List;

@ApplicationScoped
public class ActivityService {

    private ActivityRepository repository;

    protected ActivityService() {
    }

    @Inject
    public ActivityService(ActivityRepository repository) {
        this.repository = repository;
    }

    public List<ActivityEntry> getRecentActivity(Integer limit) {

        int size = limit == null ? 20 : limit;

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100."
            );
        }

        return repository.findRecent(size);
    }
}