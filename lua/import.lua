local loaded = {}
local imported = {}
luajava.loaded = loaded
luajava.imported = imported
local _G = _G
local insert = table.insert
local new = luajava.new
local bindClass = luajava.bindClass
local _M = {}

local function massage_classname(classname)
    if classname:find('_') then
        classname = classname:gsub('_', '$')
    end
    return classname
end

local function bind_class(packagename)
    local res, class = pcall(bindClass, packagename)
    if res then
        loaded[packagename] = class
        return class
    end
end

local function import_class(packagename)
    packagename = massage_classname(packagename)
    local class = loaded[packagename] or bind_class(packagename)
    return class
end

local pkgMT = {
    __index = function(T, classname)
        local ret, class = pcall(luajava.bindClass, rawget(T, "__name") .. classname)
        if ret then
            rawset(T, classname, class)
            return class
        else
            error(classname .. " is not in " .. rawget(T, "__name"), 2)
        end
    end
}

local function import_pacckage(packagename)
    local pkg = { __name = packagename }
    setmetatable(pkg, pkgMT)
    return pkg
end


--setmetatable(_G, globalMT)

local function import_require(name)
    local s, r = pcall(require, name)
    if not s and not r:find("no file") then
        error(r, 0)
    end
    return s and r
end

local function append(t, v)
    for _, _v in ipairs(t) do
        if _v == v then
            return
        end
    end
    insert(t, v)
end

local function local_import(_env, packages, package)
    local j = package:find(':')
    if j then
        local dexname = package:sub(1, j - 1)
        local classname = package:sub(j + 1, -1)
        local class = luacontext.loadDex(dexname).loadClass(classname)
        local classname = package:match('([^%.$]+)$')
        _env[classname] = class
        append(imported, package)
        return class
    end
    local i = package:find('%*$')
    if i then -- a wildcard; put into the package list, including the final '.'
        append(packages, package:sub(1, -2))
        append(imported, package)
        return import_pacckage(package:sub(1, -2))
    else
        local classname = package:match('([^%.$]+)$')
        local class = import_require(package) or import_class(package) or import_dex_class(package)
        if class then
            if class ~= true then
                --findtable(package)=class
                if type(class) ~= "table" then
                    append(imported, package)
                end
                _env[classname] = class
            end
            return class
        else
            error("cannot find " .. package, 2)
        end
    end
end


local function env_import(env)
    local _env = env or {}
    local packages = {}
    local loaders = {}
    append(packages, '')
    append(packages, 'java.lang.')
    append(packages, 'java.util.')

    local function import_1(classname)
        for i, p in ipairs(packages) do
            local class = import_class(p .. classname)
            if class then
                return class
            end
        end
    end

    append(loaders, import_1)
   

    local globalMT = {
        __index = function(T, classname)
            for i, p in ipairs(loaders) do
                local class = loaded[classname] or p(classname)
                if class then
                    T[classname] = class
                    return class
                end
            end
            return nil
        end
    }

    if type(_env)=="string" then
        return globalMT.__index({},_env)
    end

    setmetatable(_env, globalMT)
    for k, v in pairs(_M) do
        _env[k] = v
    end
    local import = function(package, env)
        env = env or _env
        if type(package) == "string" then
            return local_import(env, packages, package)
        elseif type(package) == "table" then
            local ret = {}
            for k, v in ipairs(package) do
                ret[k] = local_import(env, packages, v)
            end
            return ret
        end
    end
    _env.import = import

    return _env
end


function _M.enum(e)
    return function()
        if e.hasMoreElements() then
            return e.nextElement()
        end
    end
end

function _M.each(o)
    local iter = o.iterator()
    return function()
        if iter.hasNext() then
            return iter.next()
        end
    end
end

local NIL = {}
setmetatable(NIL, { __tostring = function() return "nil" end })

function _M.dump(o)
    local t = {}
    local _t = {}
    local _n = {}
    local space, deep = string.rep(' ', 2), 0
    local function _ToString(o, _k)
        if type(o) == ('number') then
            table.insert(t, o)
        elseif type(o) == ('string') then
            table.insert(t, string.format('%q', o))
        elseif type(o) == ('table') then
            local mt = getmetatable(o)
            if mt and mt.__tostring then
                table.insert(t, tostring(o))
            else
                deep = deep + 2
                table.insert(t, '{')

                for k, v in pairs(o) do
                    if v == _G then
                        table.insert(t, string.format('\r\n%s%s\t=%s ;', string.rep(space, deep - 1), k, "_G"))
                    elseif v ~= package.loaded then
                        if tonumber(k) then
                            k = string.format('[%s]', k)
                        else
                            k = string.format('[\"%s\"]', k)
                        end
                        table.insert(t, string.format('\r\n%s%s\t= ', string.rep(space, deep - 1), k))
                        if v == NIL then
                            table.insert(t, string.format('%s ;',"nil"))
                        elseif type(v) == ('table') then
                            if _t[tostring(v)] == nil then
                                _t[tostring(v)] = v
                                local _k = _k .. k
                                _t[tostring(v)] = _k
                                _ToString(v, _k)
                            else
                                table.insert(t, tostring(_t[tostring(v)]))
                                table.insert(t, ';')
                            end
                        else
                            _ToString(v, _k)
                        end
                    end
                end
                table.insert(t, string.format('\r\n%s}', string.rep(space, deep - 1)))
                deep = deep - 2
            end
        else
            table.insert(t, tostring(o))
        end
        table.insert(t, " ;")
        return t
    end

    t = _ToString(o, '')
    return table.concat(t)
end


function _M.printstack()
    local stacks = {}
    for m = 2, 16 do
        local dbs = {}
        local info = debug.getinfo(m)
        if info == nil then
            break
        end
        table.insert(stacks, dbs)
        dbs.info = info
        local func = info.func
        local nups = info.nups
        local ups = {}
        dbs.upvalues = ups
        for n = 1, nups do
            local n, v = debug.getupvalue(func, n)
            if v == nil then
                v = NIL
            end
            if string.byte(n) == 40 then
                if ups[n] == nil then
                    ups[n] = {}
                end
                table.insert(ups[n], v)
            else
                ups[n] = v
            end
        end

        local lps = {}
        dbs.localvalues = lps
        lps.vararg = {}
        --lps.temporary={}
        for n = -1, -255, -1 do
            local k, v = debug.getlocal(m, n)
            if k == nil then
                break
            end
            if v == nil then
                v = NIL
            end
            table.insert(lps.vararg, v)
        end
        for n = 1, 255 do
            local n, v = debug.getlocal(m, n)
            if n == nil then
                break
            end
            if v == nil then
                v = NIL
            end
            if string.byte(n) == 40 then
                if lps[n] == nil then
                    lps[n] = {}
                end
                table.insert(lps[n], v)
            else
                lps[n] = v
            end
            --table.insert(lps,string.format("%s=%s",n,v))
        end
    end
    return dump(stacks)
    -- print("info="..dump(dbs))
    -- print("_ENV="..dump(ups._ENV or lps._ENV))
end

env_import(_G)