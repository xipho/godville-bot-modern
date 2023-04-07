package ru.xipho.godvillebotmodern.repo

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.xipho.godvillebotmodern.dao.ConfigDAO

@Repository
interface ConfigRepo: CrudRepository<ConfigDAO, Long>