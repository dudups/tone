package com.ezone.ezproject.es.dao;

import java.io.IOException;

public abstract class AbstractEsIndexDocDao<K, O> extends AbstractEsBaseDao<K, O> {
    public abstract void saveOrUpdate(String index, K id, O o) throws IOException;

    @Override
    public O find(String index, K id) throws IOException {
        return find(index, id);
    }

    public void delete(String index, K id) throws IOException {
        deleteDoc(index, id);
    }
}
