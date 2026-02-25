package com.bizhub.Investistment.Services;

import com.bizhub.Investistment.Entitites.Project;

import java.sql.SQLException;
import java.util.List;

public interface IProjectService {
    int add(Project project) throws SQLException;
    boolean update(Project project) throws SQLException;
    boolean delete(int projectId) throws SQLException;

    List<Project> getAll() throws SQLException;
    List<Project> getAllWithStats() throws SQLException; // total investi + count
    Project getById(int projectId) throws SQLException;
}
