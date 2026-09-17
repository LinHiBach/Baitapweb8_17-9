/* jQuery AJAX + Bootstrap 5. Dữ liệu người dùng được render bằng .text(), không nối HTML. */
$(function () {
    const product = document.body.dataset.kind === 'products';
    const api = document.body.dataset.api;
    const categoryApi = document.body.dataset.categoryApi;
    const imageBase = document.body.dataset.images;
    const label = product ? 'sản phẩm' : 'danh mục';
    const form = document.getElementById('editorForm');
    const editorElement = document.getElementById('editor');
    const editor = new bootstrap.Modal(editorElement, {backdrop: 'static'});
    let page = 0, totalPages = 0, keyword = '', size = 5;
    let editingId = null, busy = false, listLoading = false, request = null, generation = 0;

    function notify(message, error = false, inForm = false) {
        $(inForm ? '#formNotice' : '#notice').prop('hidden', false)
            .removeClass('alert-danger alert-success').addClass(error ? 'alert-danger' : 'alert-success')
            .text(message);
    }
    function fail(xhr, inForm = false) {
        const body = xhr.responseJSON;
        notify(body?.message || (xhr.status === 0 ? 'Mất kết nối với máy chủ. Vui lòng thử lại.'
            : 'Yêu cầu thất bại (HTTP ' + xhr.status + ').'), true, inForm);
        if (inForm && body?.errors) {
            for (const [name, message] of Object.entries(body.errors)) {
                const field = form.elements.namedItem(name);
                if (field) $(field).addClass('is-invalid');
                $('[data-error]').filter(function () { return this.dataset.error === name; }).text(message);
            }
        }
    }
    function buttons() {
        $('#createButton, #rows button').prop('disabled', busy);
        $('#saveButton, #editor [data-bs-dismiss], #savedImages button').prop('disabled', busy);
        $('#previousButton').prop('disabled', busy || listLoading || page <= 0 || !totalPages);
        $('#nextButton').prop('disabled', busy || listLoading || page >= totalPages - 1);
        $('#pageNumbers button').prop('disabled', busy || listLoading);
    }
    function image(url, alt) {
        return $('<img>').addClass('thumbnail').attr({src: imageBase + encodeURIComponent(url), alt});
    }
    function renderRows(items) {
        $('#rows').empty();
        for (const item of items) {
            const row = $('<tr>').appendTo('#rows');
            $('<td>').text(item.id).appendTo(row);
            const imageCell = $('<td>').appendTo(row);
            const filename = product ? item.images?.find(i => i.primary)?.imageUrl : item.icon;
            if (filename) image(filename, item.name || item.categoryName).appendTo(imageCell);
            else $('<span>').addClass('text-secondary small').text('Chưa có ảnh').appendTo(imageCell);
            $('<td>').addClass('fw-semibold').text(product ? item.name : item.categoryName).appendTo(row);
            if (product) {
                $('<td>').addClass('text-nowrap').text(new Intl.NumberFormat('vi-VN', {
                    style: 'currency', currency: 'VND'
                }).format(item.price)).appendTo(row);
                $('<td>').text(item.quantity).appendTo(row);
                $('<td>').text(item.categoryName || 'Chưa phân loại').appendTo(row);
                $('<td>').addClass('description small text-secondary').text(item.description || '').appendTo(row);
            }
            const actions = $('<td>').addClass('text-nowrap').appendTo(row);
            $('<button>').attr('type', 'button').addClass('btn btn-sm btn-outline-primary me-2').text('Sửa')
                .on('click', () => openEditor(item.id)).appendTo(actions);
            $('<button>').attr('type', 'button').addClass('btn btn-sm btn-outline-danger').text('Xóa')
                .on('click', () => deleteItem(item)).appendTo(actions);
        }
        $('#emptyState').prop('hidden', items.length > 0);
    }
    function pagination(data) {
        $('#pageInfo').text(data.totalElements === 0 ? '0 ' + label
            : 'Trang ' + (page + 1) + ' / ' + totalPages + ' · ' + data.totalElements + ' ' + label);
        $('#pageNumbers').empty();
        // Chỉ hiện tối đa 5 số trang quanh trang hiện tại.
        let start = Math.max(0, Math.min(page - 2, totalPages - 5));
        for (let i = start; i < Math.min(totalPages, start + 5); i++) {
            const button = $('<button>').attr('type', 'button')
                .addClass('btn ' + (i === page ? 'btn-primary' : 'btn-outline-secondary'))
                .text(i + 1).on('click', () => load(i)).appendTo('#pageNumbers');
            if (i === page) button.attr('aria-current', 'page');
        }
    }
    async function load(targetPage = 0) {
        const version = ++generation;
        if (request) request.abort();
        listLoading = true;
        $('#loadingState').prop('hidden', false);
        buttons();
        try {
            request = $.ajax({url: api, dataType: 'json', data: {keyword, size, page: targetPage}});
            const response = await request;
            if (version !== generation) return;
            const data = response.data;
            if (targetPage > 0 && targetPage >= data.totalPages) {
                return await load(Math.max(0, data.totalPages - 1));
            }
            page = data.page;
            totalPages = data.totalPages;
            renderRows(data.content);
            pagination(data);
        } catch (xhr) {
            if (version === generation && xhr.statusText !== 'abort') fail(xhr);
        } finally {
            if (version === generation) {
                request = null;
                listLoading = false;
                $('#loadingState').prop('hidden', true);
                buttons();
            }
        }
    }
    async function categoryChoices() {
        if (!product) return;
        const select = $('#categoryId').empty().append($('<option>').val('').text('Chưa phân loại'));
        // Nạp tất cả các trang để danh mục thứ 101 trở đi vẫn chọn được.
        let index = 0, pages = 1;
        do {
            const response = await $.getJSON(categoryApi, {page: index, size: 100});
            response.data.content.forEach(category => select.append(
                $('<option>').val(category.id).text(category.categoryName)));
            pages = response.data.totalPages;
            index++;
        } while (index < pages);
    }
    function renderImages(item) {
        $('#savedImages').empty();
        if (!product) {
            if (item.icon) image(item.icon, item.categoryName).appendTo('#savedImages');
            return;
        }
        for (const img of item.images || []) {
            const card = $('<div>').addClass('image-card').appendTo('#savedImages');
            image(img.imageUrl, item.name).appendTo(card);
            if (img.primary) $('<div>').addClass('badge bg-success my-2').text('Ảnh chính').appendTo(card);
            $('<button>').attr('type', 'button').addClass('btn btn-sm btn-outline-danger w-100 mt-2')
                .text('Xóa ảnh').on('click', () => deleteImage(img.id)).appendTo(card);
        }
    }
    async function openEditor(id = null) {
        if (busy) return;
        busy = true;
        buttons();
        form.reset();
        $(form).find('.is-invalid').removeClass('is-invalid');
        $('[data-error]').text('');
        $('#formNotice').prop('hidden', true);
        $('#savedImages').empty();
        editingId = id;
        $('#editorTitle').text((id === null ? 'Thêm ' : 'Sửa ') + label);
        try {
            await categoryChoices();
            if (id !== null) {
                const response = await $.getJSON(api + '/' + id);
                const item = response.data;
                const names = product ? ['name', 'price', 'quantity', 'description', 'categoryId'] : ['categoryName'];
                names.forEach(name => $(form.elements.namedItem(name)).val(item[name] ?? ''));
                renderImages(item);
            }
            editor.show();
        } catch (xhr) { fail(xhr); }
        finally { busy = false; buttons(); }
    }
    async function deleteItem(item) {
        if (busy || !window.confirm('Xóa ' + label + ' "' + (item.name || item.categoryName) + '"?')) return;
        busy = true;
        buttons();
        try {
            const response = await $.ajax({url: api + '/' + item.id, method: 'DELETE', dataType: 'json'});
            notify(response.message);
            await load(page);
        } catch (xhr) { fail(xhr); }
        finally { busy = false; buttons(); }
    }
    async function deleteImage(id) {
        if (busy || !window.confirm('Xóa ảnh này?')) return;
        busy = true;
        buttons();
        try {
            await $.ajax({url: api + '/images/' + id, method: 'DELETE', dataType: 'json'});
            const response = await $.getJSON(api + '/' + editingId);
            // Chỉ cập nhật ảnh; giữ nguyên những ô nhập chưa lưu trong modal.
            renderImages(response.data);
            notify('Đã xóa ảnh.', false, true);
            await load(page);
        } catch (xhr) { fail(xhr, true); }
        finally { busy = false; buttons(); }
    }
    $('#editorForm').on('submit', async function (event) {
        event.preventDefault();
        if (busy) return;
        $(form).find('.is-invalid').removeClass('is-invalid');
        $('[data-error]').text('');
        $('#formNotice').prop('hidden', true);
        if (!form.reportValidity()) return;
        const data = new FormData(form);
        const fileInput = form.elements.namedItem(product ? 'imageFiles' : 'iconFile');
        if ([...fileInput.files].some(file => file.size > 5 * 1024 * 1024)) {
            notify('Mỗi ảnh tối đa 5 MB.', true, true); return;
        }
        if ([...fileInput.files].reduce((sum, file) => sum + file.size, 0) > 10 * 1024 * 1024) {
            notify('Tổng request tối đa 10 MB. Hãy chọn ít ảnh hơn.', true, true); return;
        }
        const create = editingId === null;
        busy = true; buttons();
        try {
            const response = await $.ajax({url: api + (create ? '' : '/' + editingId),
                method: create ? 'POST' : 'PUT', data, dataType: 'json', contentType: false, processData: false});
            busy = false;
            editor.hide();
            notify(response.message);
            if (create) { keyword = ''; $('#keyword').val(''); }
            await load(create ? 0 : page);
        } catch (xhr) { fail(xhr, true); }
        finally { busy = false; buttons(); }
    });
    editorElement.addEventListener('hide.bs.modal', event => { if (busy) event.preventDefault(); });
    editorElement.addEventListener('shown.bs.modal', () => form.querySelector('input').focus());
    $('#createButton').on('click', () => openEditor());
    $('#searchForm').on('submit', event => {
        event.preventDefault();
        keyword = $('#keyword').val().trim(); size = Number($('#size').val());
        $('#notice').prop('hidden', true); load(0);
    });
    $('#resetSearch').on('click', () => { $('#keyword').val(''); $('#searchForm').trigger('submit'); });
    $('#size').on('change', () => $('#searchForm').trigger('submit'));
    $('#previousButton').on('click', () => { if (page > 0) load(page - 1); });
    $('#nextButton').on('click', () => { if (page + 1 < totalPages) load(page + 1); });
    load(0);
});
