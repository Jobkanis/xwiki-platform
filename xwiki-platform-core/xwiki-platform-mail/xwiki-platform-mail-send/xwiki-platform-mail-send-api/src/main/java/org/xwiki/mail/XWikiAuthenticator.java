/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

import org.xwiki.stability.Unstable;

/**
 * XWiki Java Mail Authenticator taking the user name and password from a
 * {@link org.xwiki.mail.MailSenderConfiguration} instance.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Unstable
public class XWikiAuthenticator extends Authenticator
{
    private String username;

    private String password;

    /**
     * @param username the user name to use to authenticate against the SMTP server
     * @param password the password to use to authenticate against the SMTP server
     */
    public XWikiAuthenticator(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(this.username, this.password);
    }
}
