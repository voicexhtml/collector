/* Copyright 2010--2016 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.collector.bridgedescs;

import org.torproject.collector.conf.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class BridgeDescriptorParser {

  private SanitizedBridgesWriter sbw;

  private Logger logger;

  /** Initializes a new bridge descriptor parser and links it to a
   * sanitized bridges writer to sanitize and store bridge descriptors. */
  public BridgeDescriptorParser(SanitizedBridgesWriter sbw) {
    this.sbw = sbw;
    this.logger =
        LoggerFactory.getLogger(BridgeDescriptorParser.class);
  }

  /** Parses the first line of the given descriptor data to determine the
   * descriptor type and passes it to the sanitized bridges writer. */
  public void parse(byte[] allData, String dateTime)
      throws ConfigurationException {
    try {
      BufferedReader br = new BufferedReader(new StringReader(
          new String(allData, "US-ASCII")));
      String line = br.readLine();
      if (line == null) {
        return;
      } else if (line.startsWith("router ")) {
        if (this.sbw != null) {
          this.sbw.sanitizeAndStoreServerDescriptor(allData);
        }
      } else if (line.startsWith("extra-info ")) {
        if (this.sbw != null) {
          this.sbw.sanitizeAndStoreExtraInfoDescriptor(allData);
        }
      } else {
        if (this.sbw != null) {
          this.sbw.sanitizeAndStoreNetworkStatus(allData, dateTime);
        }
      }
    } catch (IOException e) {
      this.logger.warn("Could not parse bridge descriptor.", e);
      return;
    }
  }
}

