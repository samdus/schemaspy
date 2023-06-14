/*
 * Copyright (C) 2018 Nils Petzaell
 *
 * This file is part of SchemaSpy.
 *
 * SchemaSpy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SchemaSpy. If not, see <http://www.gnu.org/licenses/>.
 */
package org.schemaspy.integrationtesting.informix;

import com.github.npetzall.testcontainers.junit.jdbc.JdbcContainerRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.schemaspy.cli.CommandLineArgumentParser;
import org.schemaspy.cli.CommandLineArguments;
import org.schemaspy.input.dbms.service.DatabaseServiceFactory;
import org.schemaspy.input.dbms.service.SqlService;
import org.schemaspy.model.Database;
import org.schemaspy.model.ProgressListener;
import org.schemaspy.model.Table;
import org.schemaspy.testing.AssumeClassIsPresentExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.InformixContainer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Supplier;

import static com.github.npetzall.testcontainers.junit.jdbc.JdbcAssumptions.assumeDriverIsPresent;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nils Petzaell
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
public class InformixCheckConstraintIT {
    @Autowired
    private SqlService sqlService;

    @Mock
    private ProgressListener progressListener;

    private static Database database;

    @RegisterExtension
    public static AssumeClassIsPresentExtension jdbcDriverClassPresentExtension = new AssumeClassIsPresentExtension("com.informix.jdbc.IfxDriver");

    @SuppressWarnings("unchecked")
    public static JdbcContainerRule<InformixContainer<?>> jdbcContainerRule =
            new JdbcContainerRule<>((Supplier<InformixContainer<?>>) InformixContainer::new)
                    .assumeDockerIsPresent()
                    .withAssumptions(assumeDriverIsPresent())
                    .withInitScript("integrationTesting/informix/dbScripts/informixcc.sql");

    @ClassRule
    public static final TestRule chain = RuleChain
            .outerRule(jdbcContainerRule)
            .around(jdbcDriverClassPresentExtension);

    @Before
    public synchronized void gatheringSchemaDetailsTest() throws SQLException, IOException {
        if (database == null) {
            createDatabaseRepresentation();
        }
    }

    private void createDatabaseRepresentation() throws SQLException, IOException {
        String[] args = {
                "-t", "informix",
                "-db", "test",
                "-s", "informix",
                "-cat", "test",
                "-server", "dev",
                "-o", "target/testout/integrationtesting/informix/cc",
                "-u", jdbcContainerRule.getContainer().getUsername(),
                "-p", jdbcContainerRule.getContainer().getPassword(),
                "-host", jdbcContainerRule.getContainer().getContainerIpAddress(),
                "-port", jdbcContainerRule.getContainer().getJdbcPort().toString()
        };
        CommandLineArguments arguments = new CommandLineArgumentParser(
            new CommandLineArguments(),
            (option) -> null
        ).parse(args);
        sqlService.connect(arguments.getConnectionConfig());
        Database database = new Database(
            sqlService.getDbmsMeta(),
            arguments.getConnectionConfig().getDatabaseName(),
            arguments.getCatalog(),
            arguments.getSchema()
        );
        new DatabaseServiceFactory(sqlService).forSingleSchema(arguments.getProcessingConfig()).gatherSchemaDetails(database, null, progressListener);
        InformixCheckConstraintIT.database = database;
    }

    @Test
    public void databaseShouldBePopulatedWithTableTest() {
        Table table = getTable("test");
        assertThat(table).isNotNull();
    }

    @Test
    public void tableTesShouldContainCheckConstraint() {
        String expecting = "((((((LENGTH (firstname ) > 10 ) AND (LENGTH (lastname ) > 10 ) ) AND ((age >= 100 ) AND (age <= 105 ) ) ) AND ((weight >= 100 ) AND (weight <= 105 ) ) ) AND ((height >= 100 ) AND (height <= 105 ) ) ) OR (((((LENGTH (firstname ) > 13 ) AND (LENGTH (lastname ) > 13 ) ) AND ((age >= 106 ) AND (age <= 108 ) ) ) AND ((weight >= 106 ) AND (weight <= 108 ) ) ) AND ((height >= 106 ) AND (height <= 108 ) ) ) )";
        Table table = getTable("test");
        String actual = table.getCheckConstraints().get("big_check").trim();
        assertThat(actual).isEqualToIgnoringCase(expecting);
    }

    private Table getTable(String tableName) {
        return database.getTablesMap().get(tableName);
    }
}
