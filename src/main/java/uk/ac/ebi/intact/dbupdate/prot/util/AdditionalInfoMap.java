/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to provide additional information to entities. The key is always an AC, and the value
 * can contain different information
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class AdditionalInfoMap<V> extends HashMap<String,V> {

    public AdditionalInfoMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public AdditionalInfoMap(int initialCapacity) {
        super(initialCapacity);
    }

    public AdditionalInfoMap() {
    }

    public AdditionalInfoMap(Map<? extends String, ? extends V> m) {
        super(m);
    }
}
