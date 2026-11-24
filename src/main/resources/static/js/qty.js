/**
 * 数量步进控件（详情页与购物车共用）。
 * - 只接受正整数：拦住小数点、正负号、科学计数法，粘贴的小数也截断
 * - 聚焦全选，点一下就能覆盖
 * - ± 与手输都夹在 1—max（max 由服务端按库存给出）
 * - 所在 form 带 data-autosubmit 时，值一改就提交（购物车用）
 */
(function () {
    document.querySelectorAll('.qty').forEach(function (box) {
        var input = box.querySelector('.qty__input');
        if (!input) {
            return;
        }

        var min = Number(input.min) || 1;
        var max = Number(input.max) || min;
        var form = input.form;
        var autoSubmit = !!form && form.hasAttribute('data-autosubmit');

        var current = function () {
            var value = Math.trunc(Number(input.value));
            return isNaN(value) ? min : value;
        };
        var clamp = function (value) {
            return Math.min(Math.max(value, min), max);
        };

        var commit = function (value) {
            var next = clamp(value);
            var changed = String(next) !== input.value;
            input.value = next;
            if (changed && autoSubmit) {
                if (form.requestSubmit) {
                    form.requestSubmit();
                } else {
                    form.submit();
                }
            }
        };

        box.querySelectorAll('.qty__step').forEach(function (button) {
            button.addEventListener('click', function () {
                commit(current() + Number(button.dataset.step));
            });
        });

        input.addEventListener('focus', function () {
            input.select();
        });

        input.addEventListener('keydown', function (event) {
            if (['.', ',', 'e', 'E', '+', '-'].indexOf(event.key) >= 0) {
                event.preventDefault();
            }
        });

        input.addEventListener('input', function () {
            if (input.value.indexOf('.') >= 0) {
                input.value = String(Math.trunc(Number(input.value)) || min);
            }
        });

        input.addEventListener('change', function () {
            commit(current());
        });

        // 非自动提交的表单（详情页加购）：提交前再夹一次
        if (form && !autoSubmit) {
            form.addEventListener('submit', function () {
                input.value = clamp(current());
            });
        }
    });
})();
