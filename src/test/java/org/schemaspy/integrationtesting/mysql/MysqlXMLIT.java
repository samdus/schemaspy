/*
 * Copyright (C) 2017, 2018 Nils Petzaell
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
package org.schemaspy.integrationtesting.mysql;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.schemaspy.integrationtesting.MysqlSuite;
import org.schemaspy.testing.XmlOutputDiff;
import org.schemaspy.testing.testcontainers.SuiteContainerExtension;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.schemaspy.testing.SchemaSpyRunnerFixture.schemaSpyRunner;

/**
 * @author Nils Petzaell
 */
class MysqlXMLIT {

    private static final Path outputPath =
            Paths.get("target","testout","integrationtesting","mysql","xml");

    private static final URL expectedXML =
            MysqlXMLIT.class.getResource("/integrationTesting/mysql/expecting/mysqlxmlit/xmlit.xmlit.xml");
    private static final URL expectedDeletionOrder =
            MysqlXMLIT.class.getResource("/integrationTesting/mysql/expecting/mysqlxmlit/deletionOrder.txt");
    private static final URL expectedInsertionOrder =
            MysqlXMLIT.class.getResource("/integrationTesting/mysql/expecting/mysqlxmlit/insertionOrder.txt");

    @RegisterExtension
    static SuiteContainerExtension container = MysqlSuite.SUITE_CONTAINER;

    @BeforeAll
    static void generateXML() {
        String[] args = new String[]{
                "-t", "mysql",
                "-db", "xmlit",
                "-s", "xmlit",
                "-host", container.getHost() + ":" + container.getPort(3306),
                "-port", container.getPort(3306),
                "-u", container.getUsername(),
                "-p", container.getPassword(),
                "-nohtml",
                "-o", outputPath.toString(),
                "-connprops", "useSSL\\=false;allowPublicKeyRetrieval\\=true"
        };
        schemaSpyRunner(args).run();
    }

    @Test
    void verifyXML() {
        Diff d = XmlOutputDiff.diffXmlOutput(
                Input.fromFile(outputPath.resolve("xmlit.xmlit.xml").toString()),
                Input.fromURL(expectedXML)
                );
        assertThat(d.getDifferences()).isEmpty();
    }

    @Test
    void verifyDeletionOrder() throws IOException {
        assertThat(Files.newInputStream(outputPath.resolve("deletionOrder.txt"), StandardOpenOption.READ)).hasSameContentAs(expectedDeletionOrder.openStream());
    }

    @Test
    void verifyInsertionOrder() throws IOException {
        assertThat(Files.newInputStream(outputPath.resolve("insertionOrder.txt"), StandardOpenOption.READ)).hasSameContentAs(expectedInsertionOrder.openStream());
    }
}
